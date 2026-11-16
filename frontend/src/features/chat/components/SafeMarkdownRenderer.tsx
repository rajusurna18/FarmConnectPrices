import React, { useMemo } from 'react';

export interface SafeMarkdownRendererProps {
  content: string;
  className?: string;
}

/**
 * Parses inline formatting (bold, italic, inline code, safe links) into React elements.
 * Strictly avoids dangerouslySetInnerHTML.
 */
function parseInline(text: string): React.ReactNode[] {
  const elements: React.ReactNode[] = [];
  let remaining = text;
  let keyIndex = 0;

  while (remaining.length > 0) {
    // 1. Inline code: `code`
    const codeMatch = remaining.match(/^`([^`]+)`/);
    if (codeMatch) {
      elements.push(
        <code
          key={`code-${keyIndex++}`}
          className="px-1.5 py-0.5 bg-slate-800 text-emerald-300 rounded font-mono text-xs border border-slate-700/60"
        >
          {codeMatch[1]}
        </code>
      );
      remaining = remaining.slice(codeMatch[0].length);
      continue;
    }

    // 2. Bold text: **bold** or __bold__
    const boldMatch = remaining.match(/^(\*\*|__)(.*?)\1/);
    if (boldMatch && boldMatch[2]) {
      elements.push(
        <strong key={`bold-${keyIndex++}`} className="font-semibold text-slate-100">
          {boldMatch[2]}
        </strong>
      );
      remaining = remaining.slice(boldMatch[0].length);
      continue;
    }

    // 3. Italic text: *italic* or _italic_
    const italicMatch = remaining.match(/^(\*|_)(.*?)\1/);
    if (italicMatch && italicMatch[2]) {
      elements.push(
        <em key={`italic-${keyIndex++}`} className="italic text-slate-200">
          {italicMatch[2]}
        </em>
      );
      remaining = remaining.slice(italicMatch[0].length);
      continue;
    }

    // 4. Safe Links: [text](https://example.com)
    const linkMatch = remaining.match(/^\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/);
    if (linkMatch && linkMatch[1] && linkMatch[2]) {
      const url = linkMatch[2];
      // Only permit http: and https: protocols
      if (url.startsWith('https://') || url.startsWith('http://')) {
        elements.push(
          <a
            key={`link-${keyIndex++}`}
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-emerald-400 hover:text-emerald-300 underline underline-offset-2 transition-colors"
          >
            {linkMatch[1]}
          </a>
        );
        remaining = remaining.slice(linkMatch[0].length);
        continue;
      }
    }

    // 5. Plain character chunk up to next special delimiter
    const nextSpecial = remaining.search(/[`*_\x5b]/);
    if (nextSpecial === -1) {
      elements.push(remaining);
      break;
    } else if (nextSpecial === 0) {
      // Single delimiter without closing pair, consume 1 char as plain text
      elements.push(remaining[0]);
      remaining = remaining.slice(1);
    } else {
      elements.push(remaining.slice(0, nextSpecial));
      remaining = remaining.slice(nextSpecial);
    }
  }

  return elements;
}

/**
 * A safe, lightweight Markdown renderer for synthesized agricultural responses.
 * Never uses dangerouslySetInnerHTML; parses text strictly into React elements.
 */
export const SafeMarkdownRenderer: React.FC<SafeMarkdownRendererProps> = ({
  content,
  className = '',
}) => {
  const renderedNodes = useMemo(() => {
    if (!content) return null;

    const lines = content.split('\n');
    const nodes: React.ReactNode[] = [];
    let i = 0;
    let blockKey = 0;

    while (i < lines.length) {
      const line = lines[i];
      const trimmed = line.trim();

      // Skip blank lines
      if (!trimmed) {
        i++;
        continue;
      }

      // 1. Fenced Code Block: ```...```
      if (trimmed.startsWith('```')) {
        const codeLines: string[] = [];
        i++;
        while (i < lines.length && !lines[i].trim().startsWith('```')) {
          codeLines.push(lines[i]);
          i++;
        }
        if (i < lines.length) i++; // consume closing ```
        nodes.push(
          <pre
            key={`codeblock-${blockKey++}`}
            className="p-3 my-2 bg-slate-950 border border-slate-800 rounded-xl overflow-x-auto text-xs font-mono text-emerald-300"
          >
            <code>{codeLines.join('\n')}</code>
          </pre>
        );
        continue;
      }

      // 2. Headings: #, ##, ###, ####
      if (trimmed.startsWith('#### ')) {
        nodes.push(
          <h5 key={`h5-${blockKey++}`} className="text-xs font-bold uppercase tracking-wider text-emerald-400 mt-3 mb-1">
            {parseInline(trimmed.slice(5))}
          </h5>
        );
        i++;
        continue;
      }
      if (trimmed.startsWith('### ')) {
        nodes.push(
          <h4 key={`h4-${blockKey++}`} className="text-sm font-bold text-emerald-300 mt-3 mb-1.5 flex items-center gap-1.5">
            {parseInline(trimmed.slice(4))}
          </h4>
        );
        i++;
        continue;
      }
      if (trimmed.startsWith('## ')) {
        nodes.push(
          <h3 key={`h3-${blockKey++}`} className="text-base font-bold text-white mt-4 mb-2">
            {parseInline(trimmed.slice(3))}
          </h3>
        );
        i++;
        continue;
      }
      if (trimmed.startsWith('# ')) {
        nodes.push(
          <h2 key={`h2-${blockKey++}`} className="text-lg font-extrabold text-white mt-4 mb-2">
            {parseInline(trimmed.slice(2))}
          </h2>
        );
        i++;
        continue;
      }

      // 3. Blockquote: > text
      if (trimmed.startsWith('> ')) {
        nodes.push(
          <blockquote
            key={`quote-${blockKey++}`}
            className="border-l-3 border-emerald-500/60 pl-3 py-1 my-2 bg-slate-900/60 rounded-r-lg text-xs italic text-slate-300"
          >
            {parseInline(trimmed.slice(2))}
          </blockquote>
        );
        i++;
        continue;
      }

      // 4. Markdown Table: | col1 | col2 |
      if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
        const tableLines: string[] = [];
        while (i < lines.length && lines[i].trim().startsWith('|') && lines[i].trim().endsWith('|')) {
          tableLines.push(lines[i].trim());
          i++;
        }

        if (tableLines.length >= 2) {
          const headerCells = tableLines[0]
            .split('|')
            .slice(1, -1)
            .map((c) => c.trim());
          // Check if line 2 is separator |--|--|
          const isSeparator = /^\|?(\s*:?-+:?\s*\|?)+$/.test(tableLines[1]);
          const dataRows = (isSeparator ? tableLines.slice(2) : tableLines.slice(1)).map((row) =>
            row
              .split('|')
              .slice(1, -1)
              .map((c) => c.trim())
          );

          nodes.push(
            <div key={`table-${blockKey++}`} className="my-2.5 overflow-x-auto rounded-xl border border-slate-800 bg-slate-950/60">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-900/90 border-b border-slate-800 text-slate-300 font-semibold">
                    {headerCells.map((header, hIdx) => (
                      <th key={`th-${hIdx}`} className="px-3 py-2">
                        {parseInline(header)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60 text-slate-200">
                  {dataRows.map((row, rIdx) => (
                    <tr key={`tr-${rIdx}`} className="hover:bg-slate-900/30 transition-colors">
                      {row.map((cell, cIdx) => (
                        <td key={`td-${rIdx}-${cIdx}`} className="px-3 py-1.5">
                          {parseInline(cell)}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          );
          continue;
        }
      }

      // 5. Unordered List: - or *
      if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
        const listItems: string[] = [];
        while (i < lines.length && (lines[i].trim().startsWith('- ') || lines[i].trim().startsWith('* '))) {
          listItems.push(lines[i].trim().slice(2));
          i++;
        }
        nodes.push(
          <ul key={`ul-${blockKey++}`} className="my-2 space-y-1.5 pl-4 list-disc marker:text-emerald-400 text-xs text-slate-200">
            {listItems.map((item, itemIdx) => (
              <li key={`li-${itemIdx}`} className="leading-relaxed">
                {parseInline(item)}
              </li>
            ))}
          </ul>
        );
        continue;
      }

      // 6. Ordered List: 1. , 2.
      if (/^\d+\.\s/.test(trimmed)) {
        const listItems: string[] = [];
        while (i < lines.length && /^\d+\.\s/.test(lines[i].trim())) {
          listItems.push(lines[i].trim().replace(/^\d+\.\s/, ''));
          i++;
        }
        nodes.push(
          <ol key={`ol-${blockKey++}`} className="my-2 space-y-1.5 pl-4 list-decimal marker:text-emerald-400 text-xs text-slate-200">
            {listItems.map((item, itemIdx) => (
              <li key={`oli-${itemIdx}`} className="leading-relaxed">
                {parseInline(item)}
              </li>
            ))}
          </ol>
        );
        continue;
      }

      // 7. Standard Paragraph
      nodes.push(
        <p key={`p-${blockKey++}`} className="my-1.5 leading-relaxed text-xs text-slate-200">
          {parseInline(trimmed)}
        </p>
      );
      i++;
    }

    return nodes;
  }, [content]);

  return <div className={`prose-chat select-text ${className}`}>{renderedNodes}</div>;
};

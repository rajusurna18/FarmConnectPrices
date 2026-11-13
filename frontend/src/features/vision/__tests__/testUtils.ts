import React from 'react';

interface MockGlobalWindow {
  window?: unknown;
}

// Ensure global window exists in Node test environment
const mockGlobal = globalThis as unknown as MockGlobalWindow;
if (typeof mockGlobal.window === 'undefined') {
  mockGlobal.window = globalThis;
}

interface ReactInternals {
  H: unknown;
}

// Access React 19's client internals dispatcher
const internals = (
  React as unknown as {
    __CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE: ReactInternals;
  }
).__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE;

export interface RenderHookResult<T> {
  result: { current: T };
  rerender: () => void;
  unmount: () => void;
}

function areDepsEqual(a?: readonly unknown[], b?: readonly unknown[]): boolean {
  if (!a || !b) return false;
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (!Object.is(a[i], b[i])) return false;
  }
  return true;
}

let isBatching = false;
let pendingRender: (() => void) | null = null;

export async function act(callback: () => void | Promise<void>): Promise<void> {
  const prevBatching = isBatching;
  isBatching = true;
  try {
    await callback();
  } finally {
    isBatching = prevBatching;
    if (!isBatching && pendingRender) {
      const render = pendingRender;
      pendingRender = null;
      render();
    }
  }
}

export function renderHook<T>(callback: () => T): RenderHookResult<T> {
  const hooks: unknown[] = [];
  let hookIndex = 0;
  let effects: { idx: number; effect: () => (() => void) | void; deps?: readonly unknown[] }[] = [];
  const cleanups: Record<number, (() => void) | void> = {};
  const prevEffectDeps: Record<number, readonly unknown[] | undefined> = {};
  const result: { current: T } = { current: null as unknown as T };

  function render() {
    hookIndex = 0;
    const prevDispatcher = internals.H;
    internals.H = dispatcher;
    try {
      result.current = callback();
    } finally {
      internals.H = prevDispatcher;
    }

    const currentEffects = effects;
    effects = [];
    currentEffects.forEach(({ idx, effect, deps }) => {
      const oldDeps = prevEffectDeps[idx];
      const hasChanged = !oldDeps || !deps || !areDepsEqual(oldDeps, deps);
      if (hasChanged) {
        if (typeof cleanups[idx] === 'function') {
          cleanups[idx]!();
        }
        cleanups[idx] = effect();
        prevEffectDeps[idx] = deps;
      }
    });
  }

  const dispatcher = {
    useState<S>(initialState: S | (() => S)): [S, (action: S | ((prevState: S) => S)) => void] {
      const idx = hookIndex++;
      if (hooks[idx] === undefined) {
        hooks[idx] = typeof initialState === 'function' ? (initialState as () => S)() : initialState;
      }
      const setState = (nextState: S | ((prevState: S) => S)) => {
        const nextVal =
          typeof nextState === 'function'
            ? (nextState as (prevState: S) => S)(hooks[idx] as S)
            : nextState;
        if (!Object.is(hooks[idx], nextVal)) {
          hooks[idx] = nextVal;
          if (isBatching) {
            pendingRender = render;
          } else {
            render();
          }
        }
      };
      return [hooks[idx] as S, setState];
    },
    useRef<TVal>(initialValue: TVal): { current: TVal } {
      const idx = hookIndex++;
      if (hooks[idx] === undefined) {
        hooks[idx] = { current: initialValue };
      }
      return hooks[idx] as { current: TVal };
    },
    useCallback<F extends (...args: unknown[]) => unknown>(callbackFn: F, deps?: readonly unknown[]): F {
      const idx = hookIndex++;
      const old = hooks[idx] as { fn: F; deps?: readonly unknown[] } | undefined;
      if (old && areDepsEqual(old.deps, deps)) {
        return old.fn;
      }
      hooks[idx] = { fn: callbackFn, deps };
      return callbackFn;
    },
    useMemo<TVal>(factory: () => TVal, deps?: readonly unknown[]): TVal {
      const idx = hookIndex++;
      const old = hooks[idx] as { val: TVal; deps?: readonly unknown[] } | undefined;
      if (old && areDepsEqual(old.deps, deps)) {
        return old.val;
      }
      const val = factory();
      hooks[idx] = { val, deps };
      return val;
    },
    useEffect(effect: () => (() => void) | void, deps?: readonly unknown[]) {
      const idx = hookIndex++;
      effects.push({ idx, effect, deps });
    },
  };

  render();

  return {
    result,
    rerender: render,
    unmount: () => {
      Object.values(cleanups).forEach((c) => {
        if (typeof c === 'function') c();
      });
    },
  };
}

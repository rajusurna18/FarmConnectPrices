import React, { useState } from 'react';
import { MapPin, Activity, Network } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { IndiaMarketMap } from '../components/3d/IndiaMarketMap';
import { INDIA_MARKET_NODES, type MarketMapNode } from '../features/market/data/spatialNodeData';

export const IndiaNetworkSection: React.FC = () => {
  const [selectedNode, setSelectedNode] = useState<MarketMapNode>(INDIA_MARKET_NODES[0]);

  return (
    <section id="india-network" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <SectionHeading
          badgeText="Regional Inter-Connectivity"
          title="3D India Agricultural Market Network"
          subtitle="Visualize Mandi nodes and trade logistics linkages across key regional agricultural hubs."
        />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-center">
          
          {/* 3D Interactive Map Canvas */}
          <div className="lg:col-span-2 rounded-3xl bg-slate-900/40 border border-slate-800/80 p-4 sm:p-6 backdrop-blur-xl relative shadow-2xl">
            <div className="absolute top-4 left-4 z-10 bg-slate-950/80 px-3 py-1.5 rounded-full border border-slate-800 text-xs text-emerald-400 font-mono flex items-center space-x-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span>8 Regional Hub Nodes</span>
            </div>
            
            <IndiaMarketMap selectedNodeId={selectedNode.id} onSelectNode={(node) => setSelectedNode(node)} />
          </div>

          {/* Selected Market Node Details */}
          <div className="space-y-4">
            <GlassCard className="p-6 border-emerald-500/30">
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 flex items-center">
                  <Activity className="w-3.5 h-3.5 mr-1" />
                  {selectedNode.status}
                </span>
                <span className="text-xs text-slate-400 font-mono">NODE: {selectedNode.id.toUpperCase()}</span>
              </div>

              <h3 className="text-2xl font-extrabold text-white">{selectedNode.name} Hub</h3>
              <p className="text-xs text-slate-400 font-medium">{selectedNode.state}</p>

              <div className="pt-6 space-y-4">
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800/80">
                  <span className="text-xs text-slate-400 block">Hub Classification</span>
                  <span className="text-base font-extrabold text-white">
                    APMC Regional Wholesale Center
                  </span>
                </div>

                <div>
                  <span className="text-xs font-semibold text-slate-400 block mb-2 flex items-center">
                    <Network className="w-3.5 h-3.5 text-emerald-400 mr-1" />
                    Direct Logistics Connections ({selectedNode.connectedNodes.length})
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {selectedNode.connectedNodes.map(targetId => {
                      const connNode = INDIA_MARKET_NODES.find(n => n.id === targetId);
                      return (
                        <button
                          key={targetId}
                          onClick={() => connNode && setSelectedNode(connNode)}
                          className="px-3 py-1.5 rounded-xl bg-slate-800/80 hover:bg-emerald-600/30 text-xs font-medium text-slate-200 border border-slate-700 hover:border-emerald-500/50 transition-all flex items-center space-x-1"
                        >
                          <MapPin className="w-3 h-3 text-emerald-400" />
                          <span>{connNode?.name || targetId}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            </GlassCard>

            <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 text-xs text-slate-400 text-center">
              Regional inter-connectivity network architecture.
            </div>
          </div>

        </div>

      </div>
    </section>
  );
};


import React, { useEffect, useState, useMemo } from 'react';
import { Download, Search, ShieldAlert } from 'lucide-react';
import api from '../services/api';
import { AuditLog } from '../types';
import { useAuth } from '../context/AuthContext';

const Reports: React.FC = () => {
  const { user } = useAuth();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [tenantName, setTenantName] = useState('MeuCondominio');

  useEffect(() => {
    api.get('/tenants/audit-logs')
       .then(res => {
           setLogs(res.data || []);
           setLoading(false);
       })
       .catch(err => {
           console.error("Erro ao carregar auditoria:", err);
           setLoading(false);
       });

    if (user?.tenantId) {
        api.get('/tenants/public-list')
           .then(res => {
               const myTenant = res.data.find((t: any) => t.id === user.tenantId);
               if (myTenant) setTenantName(myTenant.nome);
           })
           .catch(err => console.error("Erro ao buscar nome do condomínio", err));
    }
  }, [user]);

  const filteredLogs = useMemo(() => {
    if (!searchTerm) return logs;
    const lowerTerm = searchTerm.toLowerCase();
    
    return logs.filter(log => 
      log.action?.toLowerCase().includes(lowerTerm) ||
      log.userName?.toLowerCase().includes(lowerTerm) ||
      log.details?.toLowerCase().includes(lowerTerm) ||
      log.userId?.toLowerCase().includes(lowerTerm)
    );
  }, [logs, searchTerm]);

  const handleExportPDF = () => {
    const originalTitle = document.title;
    const safeTenantName = tenantName.replace(/[^a-z0-9]/gi, '_');
    document.title = `relatoriovotzz_${safeTenantName}`;
    window.print();
    setTimeout(() => { document.title = originalTitle; }, 1000);
  };

  return (
    <div className="space-y-6">
      
      <style>
        {`
          @media print {
            @page { size: landscape; margin: 10mm; }
            body * { visibility: hidden; }
            #printable-area, #printable-area * { visibility: visible; }
            #printable-area { position: absolute; left: 0; top: 0; width: 100%; background: white; }
            .no-print { display: none !important; }
            
            /* Ajustes finos para impressão */
            table { width: 100% !important; table-layout: fixed !important; }
            td, th {
              white-space: normal !important;
              word-wrap: break-word !important;
              font-size: 10px !important;
              padding: 4px !important;
              border: 1px solid #ddd !important;
            }
          }
        `}
      </style>

      <div className="flex justify-between items-center no-print">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Relatórios & Auditoria</h1>
          <p className="text-slate-500">Log imutável de ações para compliance</p>
        </div>
        <button 
          onClick={handleExportPDF}
          className="flex items-center space-x-2 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        >
          <Download className="h-4 w-4" />
          <span>Exportar PDF</span>
        </button>
      </div>

      <div id="printable-area" className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50 no-print">
          <h3 className="font-semibold text-slate-700 flex items-center gap-2">
            <ShieldAlert size={18} className="text-emerald-600"/> Registro de Atividades
          </h3>
          <div className="relative">
             <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
             <input 
               type="text" 
               placeholder="Buscar..."
               value={searchTerm}
               onChange={(e) => setSearchTerm(e.target.value)}
               className="pl-9 pr-4 py-2 text-sm border border-slate-300 rounded-lg focus:ring-1 focus:ring-emerald-500 w-64"
             />
          </div>
        </div>
        
        <div className="overflow-x-auto">
          {/* CORREÇÃO: table-fixed força o respeito às larguras definidas */}
          <table className="w-full text-sm text-left table-fixed">
            <thead className="bg-slate-50 text-slate-500 uppercase text-xs font-bold">
              <tr>
                {/* Larguras ajustadas para evitar invasão */}
                <th className="px-6 py-3 w-32">Data/Hora</th>
                <th className="px-6 py-3 w-48">Ação</th>
                <th className="px-6 py-3 w-48">Responsável</th>
                <th className="px-6 py-3">Detalhes</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                  <tr><td colSpan={4} className="px-6 py-8 text-center text-slate-400 animate-pulse">Carregando registros...</td></tr>
              ) : filteredLogs.length === 0 ? (
                  <tr><td colSpan={4} className="px-6 py-8 text-center text-slate-400">Nenhum registro encontrado.</td></tr>
              ) : filteredLogs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-6 py-4 font-mono text-xs text-slate-500 font-bold align-top">
                    {new Date(log.timestamp).toLocaleString()}
                  </td>
                  
                  {/* CORREÇÃO: break-words e whitespace-normal garantem que não invade */}
                  <td className="px-6 py-4 align-top break-words whitespace-normal">
                    <span className="inline-block px-2 py-1 rounded text-[10px] font-black uppercase tracking-wide bg-blue-50 text-blue-700 border border-blue-100 break-all">
                      {log.action}
                    </span>
                  </td>
                  
                  <td className="px-6 py-4 font-medium text-slate-700 align-top break-words">
                    {log.userName || log.userId}
                  </td>
                  
                  <td className="px-6 py-4 text-slate-600 align-top break-words whitespace-normal" title={log.details}>
                    {log.details}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Reports;
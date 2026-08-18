import React, { useEffect, useState, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Calendar, ChevronRight, Users, Plus, AlertCircle, Archive, LayoutGrid, 
  Download, Trash2, Edit, MessageCircle, Mail
} from 'lucide-react';
import api from '../services/api';
import { Assembly } from '../types';
import { useAuth } from '../context/AuthContext';

const AssemblyList: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [assemblies, setAssemblies] = useState<Assembly[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'ATV' | 'HIS'>('ATV');
  const [sendingEmailId, setSendingEmailId] = useState<string | null>(null);

  const isManager = user?.role === 'MANAGER' || user?.role === 'SINDICO' || user?.role === 'ADMIN' || user?.role === 'ADM_CONDO';

  useEffect(() => {
    if (user || localStorage.getItem('@Votzz:token')) { 
      loadAssemblies();
    }
  }, [user]);

  const loadAssemblies = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.get('/assemblies');
      const data = Array.isArray(res.data) ? res.data : (res.data.data || []);
      setAssemblies(data);
    } catch (err: any) {
      console.error("Erro ao carregar lista:", err);
      setError("Não foi possível carregar as assembleias.");
    } finally {
      setLoading(false);
    }
  };

  // --- WHATSAPP ---
  const handleShareWhatsApp = (assembly: Assembly, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    const startObj = new Date(assembly.dataInicio || '');
    const startDate = startObj.toLocaleDateString('pt-BR');
    const startTime = startObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

    const endObj = assembly.dataFim ? new Date(assembly.dataFim) : null;
    const endDate = endObj ? endObj.toLocaleDateString('pt-BR') : '';
    const endTime = endObj ? endObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) : '';

    const periodString = endObj 
        ? `De ${startDate} ${startTime} até ${endDate} ${endTime}`
        : `${startDate} às ${startTime}`;
    
    // Usa window.location.origin para pegar o domínio correto
    const baseUrl = window.location.origin;
    const link = `${baseUrl}/#/voting-room/${assembly.id}`;

    const text = 
`📢 *CONVOCAÇÃO DE ASSEMBLEIA*

📌 *Título:* ${assembly.titulo}
📝 *Pauta:* ${assembly.description || 'Ver detalhes no sistema'}
📅 *Período:* ${periodString}

🔗 *Participar:* ${link}`;

    // Usa api.whatsapp.com para melhor compatibilidade de emojis/acentos
    const url = `https://api.whatsapp.com/send?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank');
  };

  // --- E-MAIL AUTOMÁTICO EM LOTES (MAILTO) ---
  const handleNotifyEmail = async (assembly: Assembly, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!window.confirm(`Deseja preparar o envio de e-mails para os moradores sobre a assembleia "${assembly.titulo}"?`)) {
        return;
    }

    setSendingEmailId(assembly.id);

    try {
        const response = await api.get('/users');
        const users = response.data || [];

        const allEmails = users
            .map((u: any) => u.email)
            .filter((email: string) => email && email.includes('@') && !email.includes('admin@votzz'))
            .filter((value: string, index: number, self: string[]) => self.indexOf(value) === index);

        if (allEmails.length === 0) {
            alert("Nenhum e-mail de morador encontrado no cadastro.");
            setSendingEmailId(null);
            return;
        }

        const BATCH_SIZE = 50; 
        const totalBatches = Math.ceil(allEmails.length / BATCH_SIZE);

        // --- PREPARAÇÃO DO TEXTO ---
        const startObj = new Date(assembly.dataInicio || '');
        const startDate = startObj.toLocaleDateString('pt-BR');
        const startTime = startObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
        
        const endObj = assembly.dataFim ? new Date(assembly.dataFim) : null;
        const endDate = endObj ? endObj.toLocaleDateString('pt-BR') : '';
        const endTime = endObj ? endObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) : '';

        const periodString = endObj 
            ? `De ${startDate} ${startTime} até ${endDate} ${endTime}`
            : `${startDate} às ${startTime}`;

        // Link para a sala
        const link = `https://www.votzz.com.br/#/voting-room/${assembly.id}`;
        
        // --- RECUPERAÇÃO DO NOME DO CONDOMÍNIO (RESOLVENDO ERRO DE TYPESCRIPT) ---
        // Usamos (user as any) para contornar a checagem estrita caso o tipo User não tenha tenantName definido explicitamente
        const condoName = (assembly as any).tenant?.nome || 
                          (user as any)?.tenant?.nome || 
                          (user as any)?.tenantName || 
                          "Seu Condomínio";

        // --- ASSUNTO DO EMAIL ---
        const subject = encodeURIComponent(`${condoName} - 📢 Convocação: ${assembly.titulo}`);
        
        // --- CORPO DO EMAIL (MAILTO) ---
        // Mailto não aceita HTML (botões), então simulamos com texto destacado
        const body = encodeURIComponent(
`Olá vizinho(a),

Você está sendo convocado(a) para uma assembleia no condomínio ${condoName}.

📌 TÍTULO: ${assembly.titulo}
📝 PAUTA: ${assembly.description || 'Ver detalhes no sistema'}
📅 PERÍODO: ${periodString}

👇 CLIQUE AQUI PARA ENTRAR NA SALA DE VOTAÇÃO:
${link}

Atenciosamente,
Administração do Condomínio "${condoName}" via Votzz.`
        );

        // Loop de Envio
        for (let i = 0; i < totalBatches; i++) {
            const start = i * BATCH_SIZE;
            const end = start + BATCH_SIZE;
            const batchEmails = allEmails.slice(start, end).join(',');

            if (totalBatches > 1) {
                const proceed = window.confirm(
                    `Pronto para enviar o LOTE ${i + 1} de ${totalBatches}?\n\n` +
                    `Destinatários: ${allEmails.slice(start, end).length} moradores.\n` +
                    `(Clique em OK para abrir seu programa de e-mail)`
                );
                if (!proceed) break;
            }

            window.location.href = `mailto:?bcc=${batchEmails}&subject=${subject}&body=${body}`;
            
            await new Promise(resolve => setTimeout(resolve, 1000));
        }

    } catch (error) {
        console.error(error);
        alert("Erro ao buscar lista de moradores. Verifique sua conexão.");
    } finally {
        setSendingEmailId(null);
    }
  };

  const handleDelete = async (id: string, e: React.MouseEvent) => {
    e.preventDefault(); 
    if (window.confirm("Tem certeza que deseja EXCLUIR esta assembleia?")) {
        try {
            await api.delete(`/assemblies/${id}`);
            setAssemblies(prev => prev.filter(a => a.id !== id));
            alert("Assembleia excluída.");
        } catch (error) {
            alert("Erro ao excluir.");
        }
    }
  };

  const handleEdit = (assembly: Assembly, e: React.MouseEvent) => {
    e.preventDefault(); 
    navigate('/create-assembly', { state: { assemblyData: assembly } });
  };

  const activeAssemblies = useMemo(() => {
    return assemblies.filter(a => {
      const s = (a.status || '').toString().toUpperCase().trim();
      return ['AGENDADA', 'SCHEDULED', 'ABERTA', 'OPEN', 'EM_ANDAMENTO', 'IN_PROGRESS', 'ACTIVE', 'ATV'].includes(s);
    });
  }, [assemblies]);
  
  const historicalAssemblies = useMemo(() => 
    assemblies.filter(a => {
      const s = (a.status || '').toString().toUpperCase().trim();
      return ['ENCERRADA', 'CLOSED', 'FINALIZADA', 'COMPLETED', 'HISTORICO'].includes(s);
    }), [assemblies]);

  const displayedAssemblies = activeTab === 'ATV' ? activeAssemblies : historicalAssemblies;

  const getStatusColor = (status: string) => {
    const s = (status || '').toUpperCase().trim();
    if (['ABERTA', 'OPEN', 'EM_ANDAMENTO'].includes(s)) return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    if (['AGENDADA', 'SCHEDULED'].includes(s)) return 'bg-blue-100 text-blue-700 border-blue-200';
    if (['ENCERRADA', 'CLOSED'].includes(s)) return 'bg-slate-100 text-slate-700 border-slate-200';
    return 'bg-amber-100 text-amber-700 border-amber-200';
  };

  const handleExportDossier = (id: string, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const apiUrl = (import.meta as any).env.VITE_API_URL || 'http://localhost:8080/api';
    window.open(`${apiUrl}/assemblies/${id}/dossier`, '_blank');
  };

  if (loading) return (
    <div className="p-20 text-center text-slate-500 animate-pulse font-bold flex flex-col items-center gap-4">
      <div className="w-8 h-8 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
      Carregando...
    </div>
  );

  return (
    <div className="space-y-6 animate-in fade-in duration-500 pb-20">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Assembleias e Votações</h1>
          <p className="text-slate-500 text-sm">Gerencie e participe das decisões do condomínio</p>
        </div>
        
        {isManager && (
          <Link 
            to="/create-assembly" 
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-all shadow-lg"
          >
            <Plus className="h-4 w-4" />
            <span>Nova Assembleia</span>
          </Link>
        )}
      </div>

      <div className="flex bg-slate-100 p-1 rounded-xl w-fit border border-slate-200">
        <button 
          onClick={() => setActiveTab('ATV')}
          className={`flex items-center gap-2 px-6 py-2 rounded-lg text-sm font-bold transition-all ${activeTab === 'ATV' ? 'bg-white shadow-sm text-emerald-600' : 'text-slate-500 hover:text-slate-700'}`}
        >
          <LayoutGrid size={16} />
          Em Andamento ({activeAssemblies.length})
        </button>
        <button 
          onClick={() => setActiveTab('HIS')}
          className={`flex items-center gap-2 px-6 py-2 rounded-lg text-sm font-bold transition-all ${activeTab === 'HIS' ? 'bg-white shadow-sm text-blue-600' : 'text-slate-500 hover:text-slate-700'}`}
        >
          <Archive size={16} />
          Realizadas ({historicalAssemblies.length})
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl flex items-center gap-3">
          <AlertCircle size={20} />
          <span className="text-sm font-medium">{error}</span>
        </div>
      )}

      <div className="grid gap-4">
        {displayedAssemblies.length > 0 ? displayedAssemblies.map((assembly) => {
          const rawDate = assembly.dataInicio || assembly.startDate;
          const displayDate = rawDate ? new Date(rawDate).toLocaleDateString('pt-BR') : 'Data a definir';
          const title = assembly.titulo || assembly.title || 'Sem título';
          const isClosed = historicalAssemblies.includes(assembly);
          
          return (
            <div key={assembly.id} className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow relative overflow-hidden group">
              {isClosed && <div className="absolute top-0 right-0 w-16 h-16 bg-slate-50 rotate-45 translate-x-8 -translate-y-8 border-l border-slate-200" />}
              
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <Link to={`/voting-room/${assembly.id}`} className="flex-1 cursor-pointer">
                  <div>
                    <div className="flex items-center space-x-3 mb-2">
                      <span className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${getStatusColor(assembly.status as string)}`}>
                        {assembly.status}
                      </span>
                      <span className="text-[10px] text-slate-400 uppercase tracking-widest font-bold">
                          {isClosed ? 'Arquivo Histórico' : 'Assembleia Digital'}
                      </span>
                    </div>
                    <h3 className={`text-lg font-bold mb-1 ${isClosed ? 'text-slate-500' : 'text-slate-800'}`}>
                      {title}
                    </h3>
                    <p className="text-slate-600 text-sm line-clamp-2 mb-4">{assembly.description}</p>
                    
                    <div className="flex items-center space-x-6 text-sm text-slate-500 font-medium">
                      <div className="flex items-center space-x-1.5">
                        <Calendar className={`h-4 w-4 ${isClosed ? 'text-slate-400' : 'text-emerald-500'}`} />
                        <span>{displayDate}</span>
                      </div>
                      <div className="flex items-center space-x-1.5">
                        <Users className={`h-4 w-4 ${isClosed ? 'text-slate-400' : 'text-blue-500'}`} />
                        <span>{assembly.votes?.length || 0} Votos</span>
                      </div>
                    </div>
                  </div>
                </Link>

                <div className="flex flex-col items-end gap-2">
                  <div className="flex flex-col md:flex-row items-center gap-2 w-full md:w-auto">
                    <Link 
                        to={`/voting-room/${assembly.id}`}
                        className={`inline-flex items-center justify-center px-4 py-2 border shadow-sm text-sm font-bold rounded-xl w-full md:w-auto transition-all ${
                            isClosed 
                            ? 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100' 
                            : 'bg-white text-slate-700 border-slate-200 hover:border-emerald-500 hover:text-emerald-600'
                        }`}
                    >
                        {isClosed ? 'Ver Resultados' : 'Entrar na Sala'}
                        <ChevronRight className="ml-1 h-4 w-4" />
                    </Link>
                  </div>

                  {isManager && (
                      <div className="flex items-center gap-2 mt-2">
                        {/* Botão WhatsApp */}
                        <button 
                            onClick={(e) => handleShareWhatsApp(assembly, e)}
                            className="p-2.5 text-green-600 bg-green-50 hover:bg-green-100 rounded-xl border border-green-200 transition-all shadow-sm"
                            title="Compartilhar no WhatsApp"
                        >
                            <MessageCircle size={18} />
                        </button>

                        {/* Botão Notificar E-mail (Batch Mailto) */}
                        <button 
                            onClick={(e) => handleNotifyEmail(assembly, e)}
                            disabled={sendingEmailId === assembly.id || isClosed}
                            className={`p-2.5 rounded-xl border transition-all shadow-sm ${
                                sendingEmailId === assembly.id 
                                ? 'bg-slate-100 text-slate-400 cursor-not-allowed' 
                                : 'text-blue-600 bg-blue-50 hover:bg-blue-100 border-blue-200'
                            }`}
                            title="Notificar todos por E-mail (Mailto)"
                        >
                            {sendingEmailId === assembly.id ? (
                                <div className="w-[18px] h-[18px] border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                            ) : (
                                <Mail size={18} />
                            )}
                        </button>

                        {!isClosed && (
                            <>
                                <button 
                                    onClick={(e) => handleEdit(assembly, e)}
                                    className="p-2.5 text-amber-500 bg-amber-50 hover:bg-amber-100 rounded-xl border border-amber-200 transition-all shadow-sm"
                                    title="Editar"
                                >
                                    <Edit size={18} />
                                </button>
                                <button 
                                    onClick={(e) => handleDelete(assembly.id, e)}
                                    className="p-2.5 text-red-500 bg-red-50 hover:bg-red-100 rounded-xl border border-red-200 transition-all shadow-sm"
                                    title="Excluir"
                                >
                                    <Trash2 size={18} />
                                </button>
                            </>
                        )}
                      </div>
                  )}

                  {isClosed && (
                    <button 
                      onClick={(e) => handleExportDossier(assembly.id, e)}
                      className="flex items-center justify-center gap-2 px-4 py-2 text-xs font-bold text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-xl transition-all w-full border border-blue-100 mt-1"
                    >
                      <Download size={14} /> Dossiê
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        }) : (
          <div className="text-center py-12 bg-white rounded-xl border border-dashed border-slate-300 text-slate-500 font-medium">
            Nenhuma assembleia encontrada nesta categoria.
          </div>
        )}
      </div>
    </div>
  );
};

export default AssemblyList;
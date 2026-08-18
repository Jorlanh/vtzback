import React, { useState, useEffect } from 'react';
import { 
  BarChart2, CheckSquare, Megaphone, Calendar as CalendarIcon, Plus, Clock, 
  AlertCircle, FileText, UserCheck, Bell, Star as StarIcon, Gavel, ShieldCheck, X, CheckCircle, ChevronRight,
  Edit2, Trash2, Download, Archive, Eye, Target, Printer, Layers
} from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

// StarIcon Component (local definition)
const StarIconComp = ({ className }: { className?: string }) => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
        <path fillRule="evenodd" d="M10.788 3.21c.448-1.077 1.976-1.077 2.424 0l2.082 5.007 5.404.433c1.164.093 1.636 1.545.749 2.305l-4.117 3.527 1.257 5.273c.271 1.136-.964 2.033-1.96 1.425L12 18.354 7.373 21.18c-.996.608-2.231-.29-1.96-1.425l1.257-5.273-4.117-3.527c-.887-.76-.415-2.212.749-2.305l5.404-.433 2.082-5.006z" clipRule="evenodd" />
    </svg>
);

interface DashboardData {
    kpis: { activePolls: number; unreadComms: number; totalActions: number; participationRate: number };
    timeline: Array<{ type: string; description: string; date: string; user: string; status?: string }>;
    calendar: Array<{ date: string; title: string; type: string; id?: string; status?: string }>;
    polls: { active: any[], archived: any[] };
    announcements: { active: any[], archived: any[] };
}

const Governance: React.FC = () => {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardData | null>(null);
  
  const [activeTab, setActiveTab] = useState<'dashboard' | 'polls' | 'comms' | 'calendar'>('dashboard');
  const [subTab, setSubTab] = useState<'active' | 'archived'>('active'); 
  const [loading, setLoading] = useState(true);

  // --- LÓGICA DE MULTI-UNIDADES ---
  const [userUnits, setUserUnits] = useState<string[]>([]);
  const [showUnitModal, setShowUnitModal] = useState(false);
  const [tempSelectedUnits, setTempSelectedUnits] = useState<string[]>([]);
  const [pendingPollVote, setPendingPollVote] = useState<{pollId: string, optionId: string} | null>(null);

  useEffect(() => {
    if (user) {
        const units = (user as any)?.unidadesList || (user?.unidade ? [user.unidade] : []);
        setUserUnits(units);
        setTempSelectedUnits(units);
    }
  }, [user]);

  const canManage = ['SINDICO', 'MANAGER', 'ADM_CONDO'].includes(user?.role || '');

  // Forms States
  const [isEditing, setIsEditing] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const [showPollModal, setShowPollModal] = useState(false);
  const [pollForm, setPollForm] = useState({ title: '', description: '', endDate: '', options: ['Sim', 'Não'], autoArchiveDate: '' });
  
  const [showAnnModal, setShowAnnModal] = useState(false);
  const [annForm, setAnnForm] = useState({ 
    title: '', content: '', priority: 'NORMAL', targetType: 'ALL', targetValue: '', requiresConfirmation: false, autoArchiveDate: '' 
  });

  const [showEventModal, setShowEventModal] = useState(false);
  const [eventForm, setEventForm] = useState({ title: '', date: '', type: 'SOCIAL' });

  useEffect(() => {
    loadDashboard();
  }, [user]);

  const loadDashboard = async () => {
    try {
        const response = await api.get('/governance/dashboard');
        const safeData = response.data || {};
        
        // Inicializa estruturas vazias se vierem nulas
        safeData.polls = safeData.polls || { active: [], archived: [] };
        safeData.announcements = safeData.announcements || { active: [], archived: [] };
        safeData.calendar = safeData.calendar || [];
        safeData.timeline = safeData.timeline || [];
        safeData.kpis = safeData.kpis || { activePolls: 0, unreadComms: 0, totalActions: 0, participationRate: 0 };

        // === FILTRO DE SEGURANÇA VISUAL (APENAS APROVADAS) ===
        // Status aceitos para exibição pública
        const allowedStatuses = ['APPROVED', 'CONFIRMED', 'COMPLETED'];
        
        // Aplica filtro na Timeline
        if (safeData.timeline) {
            safeData.timeline = safeData.timeline.filter((item: any) => {
                if (item.type !== 'BOOKING') return true; // Outros eventos passam
                // Se for booking, precisa ter status e estar na lista de permitidos
                return item.status && allowedStatuses.includes(item.status.toUpperCase());
            });
        }

        // Aplica filtro no Calendário
        if (safeData.calendar) {
            safeData.calendar = safeData.calendar.filter((item: any) => {
                if (item.type !== 'BOOKING') return true;
                return item.status && allowedStatuses.includes(item.status.toUpperCase());
            });
        }

        // Processa status de leitura dos comunicados
        const processAnnouncements = (list: any[]) => {
            return list.map(ann => {
                const userAlreadyRead = ann.isReadByCurrentUser || (ann.readBy && user?.id && ann.readBy.includes(user.id));
                return { ...ann, isReadByCurrentUser: userAlreadyRead };
            });
        };

        safeData.announcements.active = processAnnouncements(safeData.announcements.active);
        safeData.announcements.archived = processAnnouncements(safeData.announcements.archived);
        
        setData(safeData);
    } catch (e) { 
        console.error("Erro ao carregar governança:", e); 
    } finally { 
        setLoading(false); 
    }
  };

  // --- ACTIONS ---

  const handleVote = async (pollId: string, optionLabel: string) => {
      if (!data) return;

      const allPolls = [...(data.polls.active || []), ...(data.polls.archived || [])];
      const poll = allPolls.find(p => p.id === pollId);
      const option = poll?.options.find((o: any) => o.label === optionLabel);
      
      if (!option) return;

      if (userUnits.length > 1) {
          if (tempSelectedUnits.length === 0) setTempSelectedUnits(userUnits);
          setPendingPollVote({ pollId, optionId: option.id });
          setShowUnitModal(true);
      } else {
          submitPollVote(pollId, option.id, userUnits);
      }
  };

  const submitPollVote = async (pollId: string, optionId: string, unitsToVote: string[]) => {
      try {
          await api.post(`/governance/polls/${pollId}/vote`, { 
              optionId: optionId,
              units: unitsToVote
          });
          alert(`Voto registrado com sucesso para ${unitsToVote.length} unidade(s)!`);
          setShowUnitModal(false);
          loadDashboard(); 
      } catch (e: any) { 
          alert("Erro ao votar: " + (e.response?.data?.message || e.message)); 
      }
  };

  const handleConfirmRead = async (annId: string) => {
      try {
          await api.post(`/governance/announcements/${annId}/read`);
          
          setData(prevData => {
              if (!prevData) return null;
              
              const updateList = (list: any[]) => list.map(item => 
                  item.id === annId 
                  ? { 
                      ...item, 
                      isReadByCurrentUser: true, 
                      readBy: [...(item.readBy || []), user?.id] 
                    } 
                  : item
              );

              return {
                  ...prevData,
                  kpis: {
                      ...prevData.kpis,
                      unreadComms: Math.max(0, prevData.kpis.unreadComms - 1)
                  },
                  announcements: {
                      active: updateList(prevData.announcements.active),
                      archived: updateList(prevData.announcements.archived)
                  }
              };
          });

      } catch (e) { 
          console.error(e);
          alert("Erro ao confirmar leitura."); 
      }
  }

  const handleDelete = async (type: 'polls' | 'announcements' | 'events', id: string) => {
      if(!window.confirm("ATENÇÃO: Essa ação é irreversível. Deseja continuar?")) return;
      try {
          await api.delete(`/governance/${type}/${id}`);
          loadDashboard();
      } catch (e) { alert("Erro ao excluir item."); }
  };

  const handleDownloadPdf = async (pollId: string, title: string) => {
      try {
          const response = await api.get(`/governance/polls/${pollId}/report`, { 
              responseType: 'blob',
              headers: { 'Accept': 'application/pdf' }
          });

          const blob = new Blob([response.data], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          const safeTitle = title.replace(/[^a-z0-9]/gi, '_').toLowerCase();
          link.setAttribute('download', `Auditoria_${safeTitle}.pdf`);
          document.body.appendChild(link);
          link.click();
          
          setTimeout(() => {
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
          }, 100);

      } catch (e) { 
          console.error("Erro download PDF:", e);
          alert("Erro ao baixar auditoria. Verifique se o arquivo existe."); 
      }
  };

  // --- EDIT PREP ---
  const openEdit = (type: 'poll' | 'ann' | 'event', item: any) => {
      setIsEditing(true);
      setEditingId(item.id);
      
      if(type === 'poll') {
          setPollForm({ 
              title: item.title, 
              description: item.description, 
              endDate: item.endDate ? item.endDate.split('T')[0] : '',
              autoArchiveDate: item.autoArchiveDate ? item.autoArchiveDate.split('T')[0] : '', 
              options: item.options ? item.options.map((o:any) => o.label) : ['Sim', 'Não'] 
          });
          setShowPollModal(true);
      } else if (type === 'ann') {
          setAnnForm({
              title: item.title, 
              content: item.content, 
              priority: item.priority, 
              targetType: item.targetType || 'ALL',
              targetValue: item.targetValue || '',
              requiresConfirmation: item.requiresConfirmation || false,
              autoArchiveDate: item.autoArchiveDate ? item.autoArchiveDate.split('T')[0] : ''
          });
          setShowAnnModal(true);
      } else {
          setEventForm({ title: item.title, date: item.date, type: item.type });
          setShowEventModal(true);
      }
  };

  // --- SUBMITS ---
  const submitPoll = async (e: React.FormEvent) => {
      e.preventDefault();
      const formattedEndDate = pollForm.endDate.includes('T') ? pollForm.endDate : `${pollForm.endDate}T23:59:59`;
      const formattedAutoArchive = pollForm.autoArchiveDate ? (pollForm.autoArchiveDate.includes('T') ? pollForm.autoArchiveDate : `${pollForm.autoArchiveDate}T23:59:59`) : null;

      const payload = { 
          ...pollForm, 
          endDate: formattedEndDate,
          autoArchiveDate: formattedAutoArchive, 
          options: pollForm.options.map((opt: string) => ({ label: opt })) 
      };

      try {
          if(isEditing && editingId) await api.put(`/governance/polls/${editingId}`, payload);
          else await api.post('/governance/polls', payload);
          
          setShowPollModal(false); 
          loadDashboard();
      } catch(e) { alert("Erro ao salvar enquete."); }
  };

  const submitAnn = async (e: React.FormEvent) => {
      e.preventDefault();
      const formattedAutoArchive = annForm.autoArchiveDate ? (annForm.autoArchiveDate.includes('T') ? annForm.autoArchiveDate : `${annForm.autoArchiveDate}T23:59:59`) : null;
      
      const payload = { ...annForm, autoArchiveDate: formattedAutoArchive };

      try {
          if(isEditing && editingId) await api.put(`/governance/announcements/${editingId}`, payload);
          else await api.post('/governance/announcements', payload);
          
          setShowAnnModal(false); 
          loadDashboard();
      } catch(e) { alert("Erro ao salvar comunicado."); }
  };

  const submitEvent = async (e: React.FormEvent) => {
      e.preventDefault();
      try {
          if(isEditing && editingId) await api.put(`/governance/events/${editingId}`, eventForm);
          else await api.post('/governance/events', eventForm);
          
          setShowEventModal(false); 
          loadDashboard();
      } catch(e) { alert("Erro ao salvar evento."); }
  };

  const getVoteCount = (poll: any, optionId: string) => {
      if(!poll.votes) return 0;
      return poll.votes.filter((v: any) => v.optionId === optionId).length;
  };

  const toggleUnit = (unit: string) => {
    setTempSelectedUnits(prev => prev.includes(unit) ? prev.filter(u => u !== unit) : [...prev, unit]);
  };

  if (loading) return <div className="p-10 text-center text-slate-500 animate-pulse">Carregando painel de governança...</div>;
  if (!data) return <div className="p-10 text-center text-red-500">Erro ao carregar dados. Tente recarregar.</div>;

  const pollsList = data?.polls?.[subTab] || [];
  const announcementsList = data?.announcements?.[subTab] || [];

  return (
    <div className="space-y-8 animate-in fade-in pb-20">
       {/* HEADER */}
       <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
                <h1 className="text-2xl font-bold text-slate-800">Governança Digital</h1>
                <span className="bg-amber-400 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm flex items-center">
                    <StarIconComp className="w-3 h-3 mr-1" /> Plano Anual
                </span>
            </div>
            <p className="text-slate-500 text-sm">Gestão contínua, comunicados segmentados e micro-decisões.</p>
          </div>
          
          <div className="flex bg-white p-1 rounded-lg border border-slate-200 shadow-sm overflow-x-auto">
             <TabButton active={activeTab === 'dashboard'} onClick={() => setActiveTab('dashboard')} icon={BarChart2} label="Visão Geral" />
             <TabButton active={activeTab === 'polls'} onClick={() => {setActiveTab('polls'); setSubTab('active');}} icon={CheckSquare} label="Micro-decisões" />
             <TabButton active={activeTab === 'comms'} onClick={() => {setActiveTab('comms'); setSubTab('active');}} icon={Megaphone} label="Comunicados" badge={data.kpis.unreadComms > 0 ? data.kpis.unreadComms : undefined} />
             <TabButton active={activeTab === 'calendar'} onClick={() => setActiveTab('calendar')} icon={CalendarIcon} label="Calendário" />
          </div>
       </div>

       {/* === DASHBOARD KPI === */}
       {activeTab === 'dashboard' && (
           <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
               <div className="lg:col-span-3 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                  <KpiCard title="Decisões Ativas" value={data.kpis.activePolls} icon={CheckSquare} color="text-blue-500" bg="bg-white" iconBg="bg-blue-50" />
                  <KpiCard title="Comunicados Não Lidos" value={data.kpis.unreadComms} icon={Bell} color="text-orange-500" bg="bg-white" iconBg="bg-orange-50" />
                  <KpiCard title="Participação Média" value={`${data.kpis.participationRate}%`} icon={UserCheck} color="text-emerald-500" bg="bg-white" iconBg="bg-emerald-50" />
                  <KpiCard title="Ações no Período" value={data.kpis.totalActions} icon={FileText} color="text-purple-500" bg="bg-white" iconBg="bg-purple-50" />
               </div>
               
               <div className="lg:col-span-2 bg-white p-6 rounded-xl border border-slate-100 shadow-sm">
                   <h3 className="font-bold text-slate-800 mb-6 flex items-center"><Clock className="w-5 h-5 mr-2 text-slate-400" /> Linha do Tempo (Tempo Real)</h3>
                   <div className="relative pl-2">
                       <div className="absolute top-4 bottom-4 left-[21px] w-0.5 bg-slate-100 -z-0"></div>
                       {data.timeline.length === 0 && <p className="text-slate-400 text-sm ml-10">Nenhuma atividade aprovada recente.</p>}
                       {data.timeline.map((act, idx) => (
                           <div key={idx} className="relative flex items-start gap-4 mb-6 last:mb-0 z-10">
                               <div className="flex items-center justify-center w-10 h-10 rounded-full bg-white border-2 border-slate-100 shadow-sm shrink-0">
                                   {act.type === 'POLL' && <BarChart2 className="w-4 h-4 text-purple-600" />}
                                   {act.type === 'COMMUNICATION' && <Megaphone className="w-4 h-4 text-orange-600" />}
                                   {act.type === 'BOOKING' && <CalendarIcon className="w-4 h-4 text-emerald-600" />}
                                   {act.type === 'ASSEMBLY' && <Gavel className="w-4 h-4 text-blue-600" />}
                               </div>
                               <div className="bg-white p-4 rounded-lg border border-slate-100 shadow-sm flex-1 hover:border-emerald-200 transition-colors">
                                   <div className="flex justify-between items-start">
                                           <span className="font-bold text-slate-800 text-sm">{act.description}</span>
                                           <span className="text-[10px] text-slate-400 font-mono bg-slate-50 px-2 py-1 rounded">{new Date(act.date).toLocaleDateString()}</span>
                                   </div>
                                   <p className="text-xs text-slate-500 mt-1">Responsável: {act.user}</p>
                                   {act.status && <p className="text-[9px] font-bold text-emerald-600 uppercase mt-1">{act.status}</p>}
                               </div>
                           </div>
                       ))}
                   </div>
               </div>
               
               <div className="space-y-6">
                   <div className="bg-slate-900 text-white p-6 rounded-xl shadow-lg relative overflow-hidden">
                        <div className="absolute top-0 right-0 p-8 opacity-10"><ShieldCheck className="w-32 h-32" /></div>
                        <h3 className="font-bold text-lg mb-2 relative z-10">Conselho Consultivo</h3>
                        <p className="text-slate-400 text-sm mb-6 relative z-10">Área exclusiva para pré-aprovações e auditoria.</p>
                        <button className="w-full bg-emerald-600 hover:bg-emerald-500 text-white py-2 rounded-lg font-bold text-sm transition-colors relative z-10">
                            Acessar Área Restrita
                        </button>
                   </div>
               </div>
           </div>
       )}

       {/* === POLLS (ENQUETES) === */}
       {activeTab === 'polls' && (
           <div className="space-y-6 animate-in fade-in">
                <div className="flex justify-between items-center">
                    <div className="flex gap-4 items-center">
                        <h2 className="text-xl font-bold text-slate-800">Micro-decisões</h2>
                        <div className="flex bg-slate-100 rounded p-1">
                            <button onClick={() => setSubTab('active')} className={`px-3 py-1 text-xs font-bold rounded transition-all ${subTab === 'active' ? 'bg-white shadow text-emerald-700' : 'text-slate-500 hover:text-slate-700'}`}>Ativas</button>
                            <button onClick={() => setSubTab('archived')} className={`px-3 py-1 text-xs font-bold rounded transition-all ${subTab === 'archived' ? 'bg-white shadow text-slate-700' : 'text-slate-500 hover:text-slate-700'}`}>Arquivadas</button>
                        </div>
                    </div>
                    {canManage && (
                        <button onClick={() => { setIsEditing(false); setPollForm({ title: '', description: '', endDate: '', options: ['Sim', 'Não'], autoArchiveDate: '' }); setShowPollModal(true); }} className="bg-emerald-600 text-white px-4 py-2 rounded-lg font-bold flex items-center hover:bg-emerald-700 transition-colors shadow-sm">
                            <Plus className="w-4 h-4 mr-2" /> Criar Enquete
                        </button>
                    )}
                </div>
                
                {/* MODAL ENQUETE */}
                {showPollModal && (
                    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
                        <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 animate-in zoom-in-95">
                            <h3 className="font-bold text-lg mb-4">{isEditing ? 'Editar Enquete' : 'Nova Enquete'}</h3>
                            <form onSubmit={submitPoll} className="space-y-4">
                                <input type="text" placeholder="Título da Decisão" required className="w-full p-3 border rounded-lg" value={pollForm.title} onChange={e => setPollForm({...pollForm, title: e.target.value})}/>
                                <textarea placeholder="Descrição detalhada..." required className="w-full p-3 border rounded-lg" rows={3} value={pollForm.description} onChange={e => setPollForm({...pollForm, description: e.target.value})}/>
                                <div>
                                    <label className="text-xs font-bold text-slate-500 uppercase">Encerramento Votação</label>
                                    <input type="date" required className="w-full p-3 border rounded-lg mt-1" value={pollForm.endDate} onChange={e => setPollForm({...pollForm, endDate: e.target.value})}/>
                                </div>
                                <div>
                                    <label className="text-xs font-bold text-slate-500 uppercase">Arquivar Automaticamente (Data)</label>
                                    <input type="date" className="w-full p-3 border rounded-lg mt-1" value={pollForm.autoArchiveDate} onChange={e => setPollForm({...pollForm, autoArchiveDate: e.target.value})}/>
                                </div>
                                <div className="flex justify-end gap-2 mt-4 pt-4 border-t border-slate-100">
                                    <button type="button" onClick={() => setShowPollModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-50 rounded-lg">Cancelar</button>
                                    <button type="submit" className="px-6 py-2 bg-emerald-600 text-white rounded-lg font-bold hover:bg-emerald-700 shadow-sm">Salvar</button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                <div className="grid md:grid-cols-2 gap-6">
                    {pollsList.map((poll: any) => (
                        <div key={poll.id} className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm hover:shadow-md transition-shadow relative group">
                            <div className="absolute top-4 right-4 flex gap-2">
                                {canManage && (
                                    <button onClick={() => handleDownloadPdf(poll.id, poll.title)} className="p-1.5 text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 rounded transition-colors" title="Baixar Dossiê de Auditoria (PDF)"><Printer size={18}/></button>
                                )}
                                {subTab === 'archived' && !canManage && (
                                    <button onClick={() => handleDownloadPdf(poll.id, poll.title)} className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded" title="Baixar Relatório"><Download size={16}/></button>
                                )}
                                {canManage && (
                                    <>
                                        <button onClick={() => openEdit('poll', poll)} className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors" title="Editar"><Edit2 size={16}/></button>
                                        <button onClick={() => handleDelete('polls', poll.id)} className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors" title="Excluir"><Trash2 size={16}/></button>
                                    </>
                                )}
                            </div>

                            <div className="flex justify-between mb-4 pr-20">
                                <span className="bg-purple-100 text-purple-700 text-[10px] font-bold px-2 py-1 rounded">Conselho</span>
                                <div className="flex gap-2">
                                    <span className={`text-[10px] uppercase px-2 py-1 rounded font-bold ${poll.status === 'OPEN' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-500'}`}>
                                        {poll.status === 'OPEN' ? 'ABERTA' : 'ENCERRADA'}
                                    </span>
                                    {/* BADGE DE PESO MULTI-UNIDADE */}
                                    {userUnits.length > 1 && !poll.userHasVoted && poll.status === 'OPEN' && (
                                        <span className="bg-blue-100 text-blue-700 text-[10px] font-bold px-2 py-1 rounded flex items-center gap-1">
                                            <Layers size={10}/> {userUnits.length}x Voto
                                        </span>
                                    )}
                                </div>
                            </div>
                            <h3 className="font-bold text-slate-900 text-lg mb-2">{poll.title}</h3>
                            <p className="text-sm text-slate-600 mb-6">{poll.description}</p>

                            {canManage && (
                                <div className="mb-4 p-3 bg-slate-50 rounded-lg border border-slate-100">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase mb-2 flex items-center gap-1"><Target size={12}/> Auditoria Parcial</p>
                                    {poll.options.map((opt: any) => (
                                        <div key={opt.id} className="flex justify-between text-xs mb-1">
                                            <span className="text-slate-600">{opt.label}</span>
                                            <span className="font-bold text-emerald-600">{getVoteCount(poll, opt.id)} votos</span>
                                        </div>
                                    ))}
                                </div>
                            )}
                            
                            {poll.status === 'OPEN' ? (
                                <>
                                {poll.userHasVoted ? (
                                    <div className="p-4 bg-emerald-50 border border-emerald-100 rounded-lg text-center animate-in fade-in">
                                        <p className="text-emerald-700 font-bold flex items-center justify-center gap-2">
                                            <CheckCircle className="w-5 h-5"/> Voto Confirmado!
                                        </p>
                                        <p className="text-xs text-emerald-600 mt-1">Obrigado por participar.</p>
                                    </div>
                                ) : (
                                    <div className="space-y-3">
                                            {poll.options.map((opt: any) => (
                                                <button key={opt.id} onClick={() => handleVote(poll.id, opt.label)} className="w-full p-3 border rounded-lg bg-white hover:bg-slate-50 text-sm font-medium text-slate-700 text-left transition-colors border-slate-200 hover:border-emerald-300 active:bg-emerald-50 flex justify-between items-center group">
                                                    <span>{opt.label}</span>
                                                    <div className="flex items-center gap-2">
                                                        {canManage && <span className="text-xs text-slate-400 font-mono bg-slate-100 px-1 rounded">{getVoteCount(poll, opt.id)}</span>}
                                                        <span className="hidden group-hover:inline text-emerald-500 text-xs font-bold">Votar</span>
                                                    </div>
                                                </button>
                                            ))}
                                    </div>
                                )}
                                </>
                            ) : (
                                <div className="p-4 bg-slate-50 rounded-lg">
                                    <div className="text-center text-slate-500 text-sm mb-3">Votação encerrada.</div>
                                    <div className="space-y-2">
                                            {poll.options.map((opt: any) => (
                                                <div key={opt.id} className="flex justify-between text-sm text-slate-700 border-b border-slate-100 pb-1">
                                                    <span>{opt.label}</span>
                                                    <span className="font-bold">{getVoteCount(poll, opt.id)} votos</span>
                                                </div>
                                            ))}
                                    </div>
                                </div>
                            )}
                            
                            <div className="mt-4 pt-4 border-t border-slate-100 text-xs text-slate-400 flex justify-between items-center">
                                <span>Vence em: {poll.endDate ? new Date(poll.endDate).toLocaleDateString() : 'Indefinido'}</span>
                                {poll.status !== 'OPEN' && <span className="flex items-center gap-1 text-slate-500 font-medium"><Archive size={12}/> Arquivado</span>}
                            </div>
                        </div>
                    ))}
                    {pollsList.length === 0 && (
                        <div className="col-span-2 text-center py-20 text-slate-400 border-2 border-dashed border-slate-100 rounded-xl">
                            <CheckSquare className="w-12 h-12 mx-auto mb-2 opacity-20" />
                            <p>Nenhuma enquete {subTab === 'active' ? 'ativa' : 'arquivada'}.</p>
                        </div>
                    )}
                </div>
           </div>
       )}

       {/* === COMMS (COMUNICADOS) === */}
       {activeTab === 'comms' && (
           <div className="space-y-6 animate-in fade-in">
               <div className="flex justify-between items-center">
                    <div className="flex gap-4 items-center">
                        <h2 className="text-xl font-bold text-slate-800">Mural Oficial</h2>
                        <div className="flex bg-slate-100 rounded p-1">
                            <button onClick={() => setSubTab('active')} className={`px-3 py-1 text-xs font-bold rounded transition-all ${subTab === 'active' ? 'bg-white shadow text-emerald-700' : 'text-slate-500 hover:text-slate-700'}`}>Ativos</button>
                            <button onClick={() => setSubTab('archived')} className={`px-3 py-1 text-xs font-bold rounded transition-all ${subTab === 'archived' ? 'bg-white shadow text-slate-700' : 'text-slate-500 hover:text-slate-700'}`}>Arquivados</button>
                        </div>
                    </div>
                    {canManage && (
                        <button onClick={() => { setIsEditing(false); setAnnForm({ title: '', content: '', priority: 'NORMAL', targetType: 'ALL', targetValue: '', requiresConfirmation: false, autoArchiveDate: '' }); setShowAnnModal(true); }} className="bg-emerald-600 text-white px-4 py-2 rounded-lg font-bold flex items-center hover:bg-emerald-700 transition-colors shadow-sm">
                            <Plus className="w-4 h-4 mr-2" /> Novo Comunicado
                        </button>
                    )}
               </div>

               {/* MODAL COMUNICADO */}
               {showAnnModal && (
                   <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
                       <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 animate-in zoom-in-95">
                           <h3 className="font-bold text-lg mb-4">{isEditing ? 'Editar Comunicado' : 'Novo Comunicado'}</h3>
                           <form onSubmit={submitAnn} className="space-y-4">
                               <input type="text" placeholder="Título" required className="w-full p-3 border rounded-lg" value={annForm.title} onChange={e => setAnnForm({...annForm, title: e.target.value})}/>
                               <textarea placeholder="Conteúdo" required className="w-full p-3 border rounded-lg" rows={4} value={annForm.content} onChange={e => setAnnForm({...annForm, content: e.target.value})}/>
                               <div className="flex justify-between items-center bg-slate-50 p-3 rounded-lg border border-slate-100">
                                   <div className="flex-1 mr-4">
                                           <label className="block text-xs font-bold text-slate-500 mb-1 uppercase">Prioridade</label>
                                           <select className="w-full p-2 border rounded bg-white" value={annForm.priority} onChange={e => setAnnForm({...annForm, priority: e.target.value})}>
                                               <option value="NORMAL">Normal</option>
                                               <option value="HIGH">Alta Importância</option>
                                           </select>
                                   </div>
                                   <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer pt-4">
                                           <input type="checkbox" checked={annForm.requiresConfirmation} onChange={e => setAnnForm({...annForm, requiresConfirmation: e.target.checked})} className="w-4 h-4 text-emerald-600 rounded focus:ring-emerald-500"/> 
                                           Exigir Leitura
                                   </label>
                               </div>
                               <div>
                                   <label className="text-xs font-bold text-slate-500 uppercase">Arquivar Automaticamente (Data)</label>
                                   <input type="date" className="w-full p-3 border rounded-lg mt-1" value={annForm.autoArchiveDate} onChange={e => setAnnForm({...annForm, autoArchiveDate: e.target.value})}/>
                               </div>
                               <div className="flex justify-end gap-2 mt-4 pt-4 border-t border-slate-100">
                                   <button type="button" onClick={() => setShowAnnModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-50 rounded-lg">Cancelar</button>
                                   <button type="submit" className="px-6 py-2 bg-emerald-600 text-white rounded-lg font-bold hover:bg-emerald-700 shadow-sm">Salvar</button>
                               </div>
                           </form>
                       </div>
                   </div>
               )}

               <div className="space-y-4">
                   {announcementsList.map((ann: any) => (
                       <div key={ann.id} className={`p-6 rounded-xl border shadow-sm transition-all relative group ${ann.priority === 'HIGH' ? 'bg-orange-50 border-orange-100 border-l-4 border-l-orange-500' : 'bg-white border-slate-200'}`}>
                           {canManage && (
                               <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                   <button onClick={() => openEdit('ann', ann)} className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded"><Edit2 size={16}/></button>
                                   <button onClick={() => handleDelete('announcements', ann.id)} className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded"><Trash2 size={16}/></button>
                               </div>
                           )}

                           <div className="flex justify-between items-start mb-2 pr-16">
                               <div className="flex items-center gap-2">
                                   {ann.priority === 'HIGH' && <AlertCircle className="w-5 h-5 text-orange-600" />}
                                   <h3 className={`font-bold text-lg ${ann.priority === 'HIGH' ? 'text-orange-900' : 'text-slate-900'}`}>{ann.title}</h3>
                                   {ann.priority === 'HIGH' && <span className="bg-orange-200 text-orange-800 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Urgente</span>}
                                   {new Date(ann.createdAt).toDateString() === new Date().toDateString() && (
                                       <span className="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Novo</span>
                                   )}
                               </div>
                               <span className="text-xs text-slate-400">{new Date(ann.createdAt).toLocaleDateString()}</span>
                           </div>
                           
                           <p className="text-slate-600 mt-2 whitespace-pre-wrap text-sm leading-relaxed">{ann.content}</p>
                           
                           <div className="mt-4 pt-4 border-t border-slate-100/50 flex justify-between items-center">
                               <div className="flex items-center gap-2 text-xs text-slate-400">
                                   <Eye size={14}/> {ann.readBy ? ann.readBy.length : 0} leituras confirmadas
                               </div>
                               
                               {ann.requiresConfirmation && !ann.isReadByCurrentUser && (
                                   <button onClick={() => handleConfirmRead(ann.id)} className="bg-blue-50 text-blue-600 hover:bg-blue-100 px-3 py-1 rounded text-xs font-bold transition-colors border border-blue-200">
                                       Confirmar Leitura
                                   </button>
                               )}
                               {ann.isReadByCurrentUser && (
                                   <div className="flex items-center gap-1 text-emerald-600 font-bold text-xs bg-emerald-50 px-2 py-1 rounded">
                                           <CheckCircle size={12}/> Leitura Confirmada
                                   </div>
                               )}
                           </div>
                       </div>
                   ))}
                   {announcementsList.length === 0 && (
                        <div className="text-center py-20 text-slate-400 border-2 border-dashed border-slate-100 rounded-xl">
                            <Megaphone className="w-12 h-12 mx-auto mb-2 opacity-20" />
                            <p>Nenhum comunicado {subTab === 'active' ? 'ativo' : 'arquivado'}.</p>
                        </div>
                   )}
               </div>
           </div>
       )}

       {/* === TAB: CALENDÁRIO === */}
       {activeTab === 'calendar' && (
           <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm animate-in fade-in">
               <div className="flex justify-between items-start mb-8">
                   <div>
                        <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2"><CalendarIcon className="w-6 h-6 text-emerald-600" /> Calendário Unificado</h3>
                        <p className="text-slate-500 text-sm mt-1">Todas as assembleias, reservas, manutenções e eventos.</p>
                   </div>
                   {canManage && <button onClick={() => { setIsEditing(false); setEventForm({ title: '', date: '', type: 'SOCIAL' }); setShowEventModal(true); }} className="bg-slate-900 text-white px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2 hover:bg-slate-800 transition-colors shadow-sm"><Plus size={16}/> Novo Evento</button>}
               </div>

               {showEventModal && (
                   <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
                        <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 animate-in zoom-in-95">
                            <h3 className="font-bold text-lg mb-4">{isEditing ? 'Editar Evento' : 'Agendar Evento'}</h3>
                            <form onSubmit={submitEvent} className="space-y-4">
                                <input type="text" placeholder="Título do Evento" required className="w-full p-3 border rounded-lg" value={eventForm.title} onChange={e => setEventForm({...eventForm, title: e.target.value})} />
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                            <label className="text-xs font-bold text-slate-500 uppercase">Data</label>
                                            <input type="date" required className="w-full p-3 border rounded-lg mt-1" value={eventForm.date} onChange={e => setEventForm({...eventForm, date: e.target.value})} />
                                    </div>
                                    <div>
                                            <label className="text-xs font-bold text-slate-500 uppercase">Tipo</label>
                                            <select className="w-full p-3 border rounded-lg mt-1" value={eventForm.type} onChange={e => setEventForm({...eventForm, type: e.target.value})}>
                                                <option value="SOCIAL">Evento Social</option>
                                                <option value="MAINTENANCE">Manutenção</option>
                                                <option value="MEETING">Reunião</option>
                                            </select>
                                    </div>
                                </div>
                                <div className="flex justify-end gap-2 mt-4 pt-4 border-t border-slate-100">
                                    <button type="button" onClick={() => setShowEventModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-50 rounded-lg">Cancelar</button>
                                    <button type="submit" className="px-6 py-2 bg-emerald-600 text-white rounded-lg font-bold hover:bg-emerald-700 shadow-sm">Salvar</button>
                                </div>
                            </form>
                        </div>
                   </div>
               )}
               
               <div className="grid gap-4 max-w-4xl mx-auto">
                   {data.calendar.sort((a,b) => new Date(a.date).getTime() - new Date(b.date).getTime()).map((evt, idx) => (
                       <div key={idx} className="bg-white rounded-lg border border-slate-200 flex overflow-hidden hover:shadow-md transition-all relative group">
                           {canManage && !['ASSEMBLY', 'BOOKING', 'POLL'].includes(evt.type) && (
                               <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                   <button onClick={() => openEdit('event', evt)} className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded"><Edit2 size={16}/></button>
                                   {evt.id && <button onClick={() => handleDelete('events', evt.id!)} className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded"><Trash2 size={16}/></button>}
                               </div>
                           )}
                           <div className="bg-slate-50 p-4 w-24 flex flex-col items-center justify-center border-r border-slate-200 shrink-0">
                               <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">{new Date(evt.date).toLocaleString('default', { month: 'short' })}</span>
                               <span className="text-3xl font-black text-slate-900">{new Date(evt.date).getUTCDate()}</span>
                           </div>
                           <div className="p-4 flex-1 flex flex-col justify-center">
                               <h4 className="font-bold text-slate-800 text-lg">{evt.title}</h4>
                               <div className="flex gap-2 mt-2">
                                   <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wider ${
                                       evt.type === 'ASSEMBLY' ? 'bg-blue-100 text-blue-700' :
                                       evt.type === 'BOOKING' ? 'bg-emerald-100 text-emerald-700' :
                                       evt.type === 'POLL' ? 'bg-purple-100 text-purple-700' : 
                                       evt.type === 'MAINTENANCE' ? 'bg-orange-100 text-orange-700' : 'bg-slate-200 text-slate-600'
                                   }`}>
                                       {evt.type === 'ASSEMBLY' ? 'Assembleia' : 
                                        evt.type === 'BOOKING' ? 'Reserva' : 
                                        evt.type === 'POLL' ? 'Governança' : 
                                        evt.type === 'MAINTENANCE' ? 'Manutenção' : 'Evento Social'}
                                   </span>
                                   <span className="text-xs text-slate-400 flex items-center">{new Date(evt.date).toLocaleDateString()}</span>
                               </div>
                           </div>
                       </div>
                   ))}
                   {data.calendar.length === 0 && <div className="text-center py-20 text-slate-300 flex flex-col items-center"><CalendarIcon className="w-12 h-12 mb-2 opacity-50"/> Nenhum evento futuro encontrado.</div>}
               </div>
           </div>
       )}

      {/* MODAL DE SELEÇÃO DE UNIDADES (PARA MICRO-DECISÕES) */}
      {showUnitModal && (
        <div className="fixed inset-0 bg-black/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in">
          <div className="bg-white rounded-[2.5rem] p-8 max-w-sm w-full shadow-2xl border border-slate-100 scale-in-center">
            <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-black text-slate-800 flex items-center gap-2">
                    <Layers className="text-emerald-600" /> Unidades
                </h3>
                <button onClick={() => setShowUnitModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                    <X size={20} />
                </button>
            </div>
            
            <p className="text-sm text-slate-500 mb-6 font-medium text-center">Selecione por quais unidades deseja votar nesta micro-decisão:</p>
            
            <div className="space-y-2 mb-8 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
              {userUnits.map((unit: string) => (
                <label key={unit} className={`flex items-center gap-3 p-4 rounded-2xl border-2 transition-all cursor-pointer ${tempSelectedUnits.includes(unit) ? 'border-emerald-500 bg-emerald-50' : 'border-slate-100 hover:bg-slate-50'}`}>
                  <input 
                    type="checkbox" 
                    checked={tempSelectedUnits.includes(unit)}
                    onChange={() => toggleUnit(unit)}
                    className="w-5 h-5 text-emerald-600 rounded-lg border-slate-300 focus:ring-emerald-500"
                  />
                  <span className="font-bold text-slate-700">{unit}</span>
                </label>
              ))}
            </div>

            <div className="space-y-3">
              <button 
                onClick={() => pendingPollVote && submitPollVote(pendingPollVote.pollId, pendingPollVote.optionId, tempSelectedUnits)}
                disabled={tempSelectedUnits.length === 0}
                className="w-full bg-slate-900 text-white font-black py-4 rounded-2xl shadow-xl hover:bg-slate-800 disabled:opacity-30 transition-all flex items-center justify-center gap-2"
              >
                Confirmar {tempSelectedUnits.length} Voto(s)
              </button>
              <button onClick={() => setShowUnitModal(false)} className="w-full text-slate-400 font-bold py-2 hover:text-slate-600 transition-colors text-center">Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// UI Helpers
const TabButton = ({ active, onClick, icon: Icon, label, badge }: any) => (
    <button onClick={onClick} className={`flex-1 flex items-center justify-center py-3 px-4 text-sm font-medium transition-all relative ${active ? 'text-emerald-600 border-b-2 border-emerald-600' : 'text-slate-500 hover:text-slate-700 hover:bg-slate-50'}`}>
        <Icon className="w-4 h-4 mr-2" /> {label} {badge && <span className="ml-2 bg-red-500 text-white text-[10px] px-1.5 py-0.5 rounded-full">{badge}</span>}
    </button>
);

const KpiCard = ({ title, value, icon: Icon, color, bg, iconBg }: any) => (
    <div className={`p-4 rounded-xl border border-slate-100 shadow-sm flex items-center justify-between ${bg}`}>
        <div><p className="text-slate-500 text-[10px] font-bold uppercase tracking-wider mb-1">{title}</p><p className="text-3xl font-bold text-slate-800">{value}</p></div>
        <div className={`${iconBg} p-3 rounded-xl`}><Icon className={`w-6 h-6 ${color}`} /></div>
    </div>
);

export default Governance;
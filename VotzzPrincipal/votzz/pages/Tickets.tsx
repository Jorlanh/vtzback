import React, { useState, useEffect } from 'react';
import { 
  MessageSquare, Plus, Clock, User, MapPin, Send
} from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

interface Message {
  id: string;
  senderName: string;
  message: string;
  sentAt: string;
  adminSender: boolean;
}

interface Ticket {
  id: string;
  title: string;
  description: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'WAITING_TENANT' | 'RESOLVED' | 'CLOSED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  createdAt: string;
  userId: string;
  userName?: string;
  userUnit?: string;
  userBlock?: string;
  messages: Message[];
}

const Tickets: React.FC = () => {
  const { user } = useAuth();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [isCreating, setIsCreating] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false); 
  const [newTicket, setNewTicket] = useState({ title: '', description: '' });
  const [activeTicketId, setActiveTicketId] = useState<string | null>(null);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const isManager = user?.role === 'SINDICO' || user?.role === 'MANAGER' || user?.role === 'ADM_CONDO';

  useEffect(() => {
    loadTickets();
  }, []);

  const loadTickets = async () => {
    try {
      setLoading(true);
      const response = await api.get('/tickets'); 
      setTickets(response.data || []);
    } catch (error) {
      console.error("Erro ao carregar chamados", error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;

    setIsSubmitting(true);

    try {
      const payload = { 
          title: newTicket.title,
          description: newTicket.description,
          priority: 'LOW',
          status: 'OPEN'
      };

      await api.post('/tickets', payload);
      
      setIsCreating(false);
      setNewTicket({ title: '', description: '' });
      loadTickets();
      alert("Chamado aberto com sucesso!");

    } catch (error: any) {
      const errorMsg = error.response?.data?.message || error.response?.data?.error || "";
      
      // Tratamento para erro de serialização do Java (caso ocorra)
      if (errorMsg.includes("Could not write JSON") || errorMsg.includes("initialize proxy")) {
           setIsCreating(false);
           setNewTicket({ title: '', description: '' });
           loadTickets();
      } else {
           console.error("Erro detalhado:", error.response?.data);
           alert("Erro ao abrir chamado: " + (errorMsg || "Verifique os dados."));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSendMessage = async (ticketId: string) => {
    if (!newMessage.trim()) return;
    try {
      await api.post(`/tickets/${ticketId}/messages`, { message: newMessage });
      setNewMessage('');
      loadTickets();
    } catch (error) {
      alert("Erro ao enviar mensagem.");
    }
  };

  const handlePriorityChange = async (ticketId: string, newPriority: string) => {
    try {
      await api.patch(`/tickets/${ticketId}/priority`, { priority: newPriority });
      loadTickets();
    } catch (e) {
      alert("Erro ao mudar prioridade");
    }
  };

  const handleStatusChange = async (ticketId: string, newStatus: string) => {
    try {
        await api.patch(`/tickets/${ticketId}/status`, { status: newStatus });
        loadTickets();
      } catch (e) {
        alert("Erro ao mudar status");
      }
  }

  const getStatusColor = (status: string) => {
    const map: Record<string, string> = {
      'OPEN': 'bg-blue-100 text-blue-800 border-blue-200',
      'IN_PROGRESS': 'bg-amber-100 text-amber-800 border-amber-200',
      'WAITING_TENANT': 'bg-purple-100 text-purple-800 border-purple-200',
      'RESOLVED': 'bg-emerald-100 text-emerald-800 border-emerald-200',
      'CLOSED': 'bg-slate-100 text-slate-600 border-slate-200'
    };
    return map[status] || 'bg-gray-100';
  };

  const getPriorityBadge = (priority: string) => {
    const map: Record<string, string> = {
      'LOW': 'text-slate-500 bg-slate-100',
      'MEDIUM': 'text-amber-600 bg-amber-50 border-amber-100',
      'HIGH': 'text-orange-600 bg-orange-50 border-orange-100 font-bold',
      'URGENT': 'text-red-600 bg-red-50 border-red-100 font-black animate-pulse'
    };
    return `text-xs px-2 py-1 rounded border ${map[priority] || ''}`;
  };

  return (
    <div className="space-y-6 animate-in fade-in pb-20">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Central de Chamados</h1>
          <p className="text-slate-500 text-sm">Gerenciamento e comunicação direta.</p>
        </div>
        <button 
          onClick={() => setIsCreating(!isCreating)} 
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 font-bold shadow-md transition-all active:scale-95"
        >
          {isCreating ? 'Fechar' : <><Plus size={18} /> Novo Chamado</>}
        </button>
      </div>

      {isCreating && (
        <div className="bg-white p-6 rounded-xl border border-blue-100 shadow-lg animate-in slide-in-from-top-2">
          <form onSubmit={handleCreateTicket} className="space-y-4">
            <div>
              <label className="block text-sm font-bold text-slate-700 mb-1">Título do Problema</label>
              <input 
                type="text" required value={newTicket.title} 
                onChange={e => setNewTicket({...newTicket, title: e.target.value})} 
                className="w-full p-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="Ex: Vazamento na garagem"
                disabled={isSubmitting}
              />
            </div>
            <div>
              <label className="block text-sm font-bold text-slate-700 mb-1">Descrição</label>
              <textarea 
                required rows={3} value={newTicket.description} 
                onChange={e => setNewTicket({...newTicket, description: e.target.value})} 
                className="w-full p-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="Detalhe o ocorrido..."
                disabled={isSubmitting}
              />
            </div>
            <div className="flex justify-end">
              <button 
                type="submit" 
                disabled={isSubmitting}
                className={`text-white px-6 py-2 rounded-lg font-bold transition-all ${
                    isSubmitting 
                    ? 'bg-slate-400 cursor-not-allowed' 
                    : 'bg-blue-600 hover:bg-blue-700'
                }`}
              >
                {isSubmitting ? 'Enviando...' : 'Enviar'}
              </button>
            </div>
          </form>
        </div>
      )}

      {loading ? <div className="text-center py-10 text-slate-400">Carregando...</div> : (
        <div className="space-y-4">
          {tickets.map(ticket => (
            <div key={ticket.id} className={`bg-white rounded-xl border transition-all ${activeTicketId === ticket.id ? 'ring-2 ring-blue-500 border-transparent shadow-lg' : 'border-slate-200 hover:shadow-md'}`}>
              
              <div className="p-5 cursor-pointer" onClick={() => setActiveTicketId(activeTicketId === ticket.id ? null : ticket.id)}>
                <div className="flex flex-col md:flex-row justify-between md:items-start gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wide border ${getStatusColor(ticket.status)}`}>
                        {ticket.status.replace('_', ' ')}
                      </span>
                      <span className={getPriorityBadge(ticket.priority)}>
                        {ticket.priority}
                      </span>
                      <span className="text-xs text-slate-400 flex items-center gap-1">
                        <Clock size={12} /> {new Date(ticket.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        {ticket.title} 
                        <MessageSquare size={16} className="text-slate-300" />
                        <span className="text-xs text-slate-400 font-normal">({ticket.messages?.length || 0})</span>
                    </h3>
                    
                    <div className="mt-2 text-xs text-slate-500 flex items-center gap-3">
                        <span className="flex items-center gap-1"><User size={12} /> {ticket.userName}</span>
                        {(ticket.userBlock || ticket.userUnit) && (
                            <span className="flex items-center gap-1"><MapPin size={12} /> {ticket.userBlock}/{ticket.userUnit}</span>
                        )}
                    </div>
                  </div>

                  {isManager && (
                      <div className="flex flex-col items-end gap-2" onClick={e => e.stopPropagation()}>
                          <select 
                            value={ticket.priority}
                            onChange={(e) => handlePriorityChange(ticket.id, e.target.value)}
                            className="text-xs bg-slate-50 border border-slate-200 rounded p-1 outline-none focus:border-blue-500"
                          >
                              <option value="LOW">Baixa</option>
                              <option value="MEDIUM">Média</option>
                              <option value="HIGH">Alta</option>
                              <option value="URGENT">Urgente</option>
                          </select>
                      </div>
                  )}
                </div>
              </div>

              {activeTicketId === ticket.id && (
                <div className="border-t border-slate-100 bg-slate-50/50 rounded-b-xl">
                  <div className="p-5 border-b border-slate-100">
                    <h4 className="text-xs font-bold text-slate-400 uppercase mb-2">Descrição Detalhada</h4>
                    <p className="text-slate-700 text-sm whitespace-pre-wrap">{ticket.description}</p>
                    
                    <div className="mt-4 flex gap-2">
                        {isManager && ticket.status !== 'CLOSED' && (
                            <>
                                <button onClick={() => handleStatusChange(ticket.id, 'IN_PROGRESS')} className="text-xs bg-white border px-3 py-1 rounded hover:bg-slate-50">Assumir</button>
                                <button onClick={() => handleStatusChange(ticket.id, 'RESOLVED')} className="text-xs bg-emerald-600 text-white px-3 py-1 rounded hover:bg-emerald-700">Resolver</button>
                            </>
                        )}
                        {ticket.status !== 'CLOSED' && (
                             <button onClick={() => handleStatusChange(ticket.id, 'CLOSED')} className="text-xs bg-slate-200 text-slate-700 px-3 py-1 rounded hover:bg-slate-300">Encerrar</button>
                        )}
                    </div>
                  </div>

                  <div className="p-5">
                    <h4 className="text-xs font-bold text-slate-400 uppercase mb-4 flex items-center gap-2">
                        Histórico de Atendimento <MessageSquare size={12} />
                    </h4>
                    
                    <div className="space-y-4 max-h-[300px] overflow-y-auto mb-4 pr-2">
                        {ticket.messages?.length === 0 && <p className="text-center text-xs text-slate-400 italic">Nenhuma mensagem ainda.</p>}
                        
                        {ticket.messages?.map(msg => {
                            const isMe = msg.senderName === user?.nome; 
                            const isStaff = msg.adminSender;
                            
                            return (
                                <div key={msg.id} className={`flex flex-col ${isMe ? 'items-end' : 'items-start'}`}>
                                    <div className={`max-w-[85%] p-3 rounded-2xl text-sm shadow-sm relative ${
                                        isMe 
                                        ? 'bg-blue-600 text-white rounded-tr-none' 
                                        : isStaff 
                                            ? 'bg-slate-800 text-white rounded-tl-none border border-slate-700' 
                                            : 'bg-white text-slate-700 border border-slate-200 rounded-tl-none' 
                                    }`}>
                                        {isStaff && !isMe && <span className="text-[10px] font-bold text-emerald-400 block mb-1">ADMINISTRAÇÃO</span>}
                                        <p>{msg.message}</p>
                                    </div>
                                    <span className="text-[10px] text-slate-400 mt-1 px-1">
                                        {isMe ? 'Você' : msg.senderName} • {new Date(msg.sentAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                                    </span>
                                </div>
                            );
                        })}
                    </div>

                    {ticket.status !== 'CLOSED' && (
                        <div className="flex gap-2">
                            <input 
                                type="text" 
                                value={newMessage}
                                onChange={e => setNewMessage(e.target.value)}
                                onKeyDown={e => e.key === 'Enter' && handleSendMessage(ticket.id)}
                                placeholder="Digite uma mensagem..."
                                className="flex-1 text-sm p-3 border border-slate-300 rounded-lg outline-none focus:border-blue-500 shadow-sm"
                            />
                            <button 
                                onClick={() => handleSendMessage(ticket.id)}
                                className="bg-blue-600 text-white p-3 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
                            >
                                <Send size={18} />
                            </button>
                        </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Tickets;
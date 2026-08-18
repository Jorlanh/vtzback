import React, { useState, useMemo, useEffect } from 'react';
import { 
  Package, Search, Plus, User, Clock, Trash2, Smartphone, 
  X, SearchX, Archive, Mail, Shield, AlertCircle, CheckCircle, 
  MapPin, Building, Truck, AtSign, Phone, Edit, Save
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

// --- TIPAGENS ---
interface Resident {
  id: string;
  nome: string;
  email: string;
  cpf?: string;
  whatsapp?: string;
  unidade?: string;
  bloco?: string;
}

interface Order {
  id: string;
  trackingCode: string;
  origin: string;
  recipientName: string;
  arrivalDate: string;
  status: 'PENDING' | 'ARCHIVED'; 
  
  // Snapshot do Morador
  residentId: string;
  residentName: string;
  unit: string;
  block: string;
  residentEmail: string;
  residentWhatsapp: string;

  // Assinaturas
  residentSignatureDate?: string;
  residentSignatureName?: string;
  staffSignatureDate?: string;
  staffSignatureName?: string;
}

const Orders: React.FC = () => {
  const { user } = useAuth();
  
  // Permissões
  const isStaff = ['SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO'].includes(user?.role || '');

  const [orders, setOrders] = useState<Order[]>([]);
  const [residentsList, setResidentsList] = useState<Resident[]>([]);
  const [activeTab, setActiveTab] = useState<'ACTIVE' | 'ARCHIVED'>('ACTIVE');
  
  // Modal e Formulário
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [residentSearch, setResidentSearch] = useState('');
  
  // Estado para Edição: Se tiver ID, é edição. Se null, é criação.
  const [editingOrderId, setEditingOrderId] = useState<string | null>(null);

  // Estado único para todo o formulário
  const [formData, setFormData] = useState({
    residentId: '',
    residentName: '',
    block: '',
    unit: '',
    email: '',
    whatsapp: '',
    trackingCode: '',
    origin: '',
    recipientName: '',
    arrivalDate: '' 
  });

  // --- 1. CARREGAMENTO DE DADOS ---
  useEffect(() => {
    loadData();
  }, [user]);

  const loadData = async () => {
    if (!user) return;
    try {
        setIsLoading(true);
        const ordersRes = await api.get('/orders').catch(() => ({ data: [] })); 
        setOrders(ordersRes.data || []);

        if (isStaff) {
            const usersRes = await api.get('/users').catch(() => ({ data: [] }));
            const validResidents = usersRes.data.filter((u: any) => u.role !== 'ADMIN' && u.nome);
            setResidentsList(validResidents);
        }
    } catch (error) {
        console.error("Erro ao carregar dados", error);
    } finally {
        setIsLoading(false);
    }
  };

  // --- 2. LÓGICA DE PESQUISA DE MORADOR ---
  const filteredResidents = useMemo(() => {
    if (!residentSearch) return [];
    const searchLower = residentSearch.toLowerCase();
    return residentsList.filter(r => 
      r.nome.toLowerCase().includes(searchLower) || 
      (r.unidade && r.unidade.includes(searchLower)) ||
      (r.bloco && r.bloco.toLowerCase().includes(searchLower))
    );
  }, [residentSearch, residentsList]);

  const handleSelectResident = (resident: Resident) => {
    setFormData(prev => ({
      ...prev,
      residentId: resident.id,
      residentName: resident.nome,
      block: resident.bloco || '',
      unit: resident.unidade || '',
      email: resident.email,
      whatsapp: resident.whatsapp || '',
      recipientName: resident.nome 
    }));
    setResidentSearch('');
  };

  // --- 3. ABRIR MODAL (CRIAÇÃO VS EDIÇÃO) ---
  const openCreateModal = () => {
    setEditingOrderId(null);
    setFormData({
        residentId: '', residentName: '', block: '', unit: '', email: '', whatsapp: '',
        trackingCode: '', origin: '', recipientName: '', arrivalDate: ''
    });
    setIsModalOpen(true);
  };

  const openEditModal = (order: Order) => {
    setEditingOrderId(order.id);
    setFormData({
        residentId: order.residentId,
        residentName: order.residentName,
        block: order.block,
        unit: order.unit,
        email: order.residentEmail,
        whatsapp: order.residentWhatsapp,
        trackingCode: order.trackingCode,
        origin: order.origin || '',
        recipientName: order.recipientName,
        arrivalDate: order.arrivalDate ? order.arrivalDate.substring(0, 16) : '' 
    });
    setIsModalOpen(true);
  };

  // --- 4. SALVAR (CREATE OR UPDATE) ---
  const handleSaveOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.residentId) return alert("Por favor, localize e selecione um morador na base.");

    try {
      setIsLoading(true);
      const payload = {
        trackingCode: formData.trackingCode || 'S/C',
        origin: formData.origin || 'Não Identificado',
        recipientName: formData.recipientName,
        arrivalDate: formData.arrivalDate || new Date().toISOString(),
        status: 'PENDING',
        residentId: formData.residentId,
        residentName: formData.residentName,
        unit: formData.unit || 'N/A',
        block: formData.block || 'N/A',
        residentEmail: formData.email,
        residentWhatsapp: formData.whatsapp || ''
      };

      let savedOrder: Order;
      
      if (editingOrderId) {
          // UPDATE
          const res = await api.put(`/orders/${editingOrderId}`, payload);
          savedOrder = res.data;
          setOrders(prev => prev.map(o => o.id === editingOrderId ? savedOrder : o));
          alert("Encomenda atualizada com sucesso!");
      } else {
          // CREATE
          const res = await api.post('/orders', payload);
          savedOrder = res.data;
          setOrders([savedOrder, ...orders]);
          alert("Encomenda registrada com sucesso!");
      }

      setIsModalOpen(false);
      setResidentSearch('');
      setFormData({
        residentId: '', residentName: '', block: '', unit: '', email: '', whatsapp: '',
        trackingCode: '', origin: '', recipientName: '', arrivalDate: ''
      });

    } catch (error) {
      alert("Erro ao salvar. Verifique a conexão.");
    } finally {
      setIsLoading(false);
    }
  };

  // --- 5. AÇÕES ---
  const handleSignature = async (orderId: string, type: 'RESIDENT' | 'STAFF') => {
    if (!confirm(`Confirma a assinatura eletrônica como ${type === 'RESIDENT' ? 'Morador' : 'Portaria'}?`)) return;
    try {
        const res = await api.put(`/orders/${orderId}/sign`, { type }); 
        const updatedOrder = res.data;
        setOrders(prev => prev.map(o => o.id === orderId ? updatedOrder : o));
    } catch (error) { 
        alert("Erro ao processar assinatura."); 
    }
  };

  const handleDelete = async (id: string, tracking: string) => {
    if (!confirm("Tem certeza? Esta ação será auditada.")) return;
    try {
        await api.delete(`/orders/${id}`);
        setOrders(prev => prev.filter(o => o.id !== id));
    } catch (e) { alert("Erro ao excluir."); }
  };

  // --- 6. NOTIFICAÇÕES (CLIENT-SIDE) ---
  const sendWhatsApp = (order: Order) => {
    if (!order.residentWhatsapp) return alert("Morador sem WhatsApp cadastrado.");
    const text = `Olá ${order.residentName}! 📦\n\nSua encomenda chegou na portaria.\n*Origem:* ${order.origin}\n*Código:* ${order.trackingCode}\n\nDisponível para retirada.`;
    window.open(`https://wa.me/55${order.residentWhatsapp.replace(/\D/g,'')}?text=${encodeURIComponent(text)}`, '_blank');
  };

  // USA MAILTO: PARA ABRIR O CLIENTE DE EMAIL LOCAL
  const sendEmailNotification = (order: Order) => {
     if (!order.residentEmail) return alert("Morador sem E-mail cadastrado.");
     
     const subject = `Votzz: Chegou Encomenda - ${order.trackingCode}`;
     const body = `Olá ${order.residentName},\n\nSua encomenda chegou na portaria do condomínio.\n\nOrigem: ${order.origin}\nCódigo: ${order.trackingCode}\n\nPor favor, retire na portaria.\n\nAtenciosamente,\nAdministração.`;
     
     const mailtoLink = `mailto:${order.residentEmail}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
     window.location.href = mailtoLink;
  };

  // Filtros
  const displayedOrders = orders.filter(o => {
     const isArchived = o.status === 'ARCHIVED' || (o.residentSignatureDate && o.staffSignatureDate);
     return activeTab === 'ARCHIVED' ? isArchived : !isArchived;
  });

  return (
    <div className="p-6 md:p-8 bg-slate-50 min-h-screen animate-in fade-in duration-500">
      
      {/* HEADER */}
      <div className="flex flex-col md:flex-row justify-between items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-3">
            <Package className="text-emerald-600" size={32} /> Gestão de Encomendas
          </h1>
          <p className="text-slate-500 mt-1">Controle de recebimento, entrega e auditoria.</p>
        </div>
        {isStaff && (
            <button onClick={openCreateModal} className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-3 rounded-xl font-bold shadow-lg flex items-center gap-2 transition-all hover:-translate-y-1">
               <Plus size={20} /> Nova Encomenda
            </button>
        )}
      </div>

      {/* ABAS */}
      <div className="flex gap-6 mb-6 border-b border-slate-200">
        <button onClick={() => setActiveTab('ACTIVE')} className={`pb-3 px-2 font-bold flex items-center gap-2 transition-colors ${activeTab === 'ACTIVE' ? 'border-b-4 border-emerald-500 text-emerald-700' : 'text-slate-400'}`}>
          <Clock size={18} /> Aguardando Retirada
        </button>
        <button onClick={() => setActiveTab('ARCHIVED')} className={`pb-3 px-2 font-bold flex items-center gap-2 transition-colors ${activeTab === 'ARCHIVED' ? 'border-b-4 border-emerald-500 text-emerald-700' : 'text-slate-400'}`}>
          <Archive size={18} /> Arquivadas
        </button>
      </div>

      {/* LISTAGEM */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {displayedOrders.length === 0 && (
          <div className="col-span-full py-20 text-center text-slate-400 bg-white rounded-3xl border border-slate-100 border-dashed">
            <Package size={48} className="mx-auto mb-3 opacity-20" />
            <p>Nenhuma encomenda nesta categoria.</p>
          </div>
        )}

        {displayedOrders.map(order => (
          <div key={order.id} className="bg-white rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition-all flex flex-col overflow-hidden group">
            
            {/* CABEÇALHO DO CARD */}
            <div className="p-4 border-b border-slate-100 bg-slate-50/50 flex justify-between items-start">
                <div className="flex gap-3">
                    <div className="bg-blue-100 p-2 rounded-lg text-blue-600 h-fit"><Truck size={20} /></div>
                    <div>
                        <p className="text-[10px] font-black text-slate-400 uppercase tracking-wider">{order.origin}</p>
                        <p className="font-mono font-bold text-slate-800 text-sm">{order.trackingCode}</p>
                        <p className="text-[10px] text-slate-500 mt-1 flex items-center gap-1">
                            <Clock size={10}/> {new Date(order.arrivalDate).toLocaleDateString()} {new Date(order.arrivalDate).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}
                        </p>
                    </div>
                </div>
                {isStaff && (
                    <div className="flex gap-1">
                        <button onClick={() => openEditModal(order)} className="text-slate-300 hover:text-blue-500 p-1.5 hover:bg-blue-50 rounded-lg transition-colors" title="Editar">
                           <Edit size={16} />
                        </button>
                        <button onClick={() => handleDelete(order.id, order.trackingCode)} className="text-slate-300 hover:text-red-500 p-1.5 hover:bg-red-50 rounded-lg transition-colors" title="Excluir">
                           <Trash2 size={16} />
                        </button>
                    </div>
                )}
            </div>

            {/* CORPO DO CARD */}
            <div className="p-4 flex-1">
                <div className="pl-2 border-l-2 border-emerald-100 mb-4">
                    <p className="text-xs text-slate-400 font-bold uppercase mb-1">Destinatário</p>
                    <p className="font-bold text-slate-800">{order.recipientName}</p>
                    <div className="flex gap-2 text-xs text-slate-500 mt-1">
                        <span className="flex items-center gap-1"><Building size={10}/> Bloco {order.block}</span>
                        <span className="flex items-center gap-1"><MapPin size={10}/> Apto {order.unit}</span>
                    </div>
                </div>
                
                {/* BOTÕES DE NOTIFICAÇÃO (APENAS ATIVOS) */}
                {activeTab === 'ACTIVE' && (
                    <div className="grid grid-cols-2 gap-2 mb-4">
                        <button onClick={() => sendWhatsApp(order)} className="bg-green-50 text-green-700 hover:bg-green-100 p-2 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-colors"><Smartphone size={14}/> WhatsApp</button>
                        
                        {/* ALTERADO PARA USAR MAILTO LOCAL */}
                        <button onClick={() => sendEmailNotification(order)} className="bg-blue-50 text-blue-700 hover:bg-blue-100 p-2 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-colors"><Mail size={14}/> E-mail</button>
                    </div>
                )}

                {/* ASSINATURAS */}
                <div className="space-y-2 pt-3 border-t border-slate-100">
                    <div className={`flex justify-between items-center p-2 rounded-lg text-xs border ${order.residentSignatureDate ? 'bg-emerald-50 border-emerald-200 text-emerald-800' : 'bg-slate-50 border-slate-100 text-slate-400'}`}>
                        <div className="flex flex-col">
                            <span className="font-bold flex items-center gap-1 mb-0.5">
                                {order.residentSignatureDate ? <CheckCircle size={12}/> : <AlertCircle size={12}/>} Morador
                            </span>
                            {order.residentSignatureDate && (
                                <span className="font-bold text-[10px] uppercase tracking-wide opacity-80">{order.residentSignatureName}</span>
                            )}
                        </div>
                        {order.residentSignatureDate ? (
                             <span className="text-[10px] font-mono text-right">
                                {new Date(order.residentSignatureDate).toLocaleDateString()}<br/>
                                {new Date(order.residentSignatureDate).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                            </span>
                        ) : <button onClick={() => handleSignature(order.id, 'RESIDENT')} className="bg-white border hover:border-emerald-500 px-2 py-1 rounded font-bold text-slate-600 transition-colors">Assinar</button>}
                    </div>

                    <div className={`flex justify-between items-center p-2 rounded-lg text-xs border ${order.staffSignatureDate ? 'bg-emerald-50 border-emerald-200 text-emerald-800' : 'bg-slate-50 border-slate-100 text-slate-400'}`}>
                        <div className="flex flex-col">
                            <span className="font-bold flex items-center gap-1 mb-0.5">
                                {order.staffSignatureDate ? <CheckCircle size={12}/> : <Shield size={12}/>} Portaria
                            </span>
                            {order.staffSignatureDate && (
                                <span className="font-bold text-[10px] uppercase tracking-wide opacity-80">{order.staffSignatureName}</span>
                            )}
                        </div>
                        {order.staffSignatureDate ? (
                             <span className="text-[10px] font-mono text-right">
                                {new Date(order.staffSignatureDate).toLocaleDateString()}<br/>
                                {new Date(order.staffSignatureDate).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                            </span>
                        ) : isStaff && <button onClick={() => handleSignature(order.id, 'STAFF')} className="bg-white border hover:border-emerald-500 px-2 py-1 rounded font-bold text-slate-600 transition-colors">Confirmar</button>}
                    </div>
                </div>
            </div>
          </div>
        ))}
      </div>

      {/* MODAL (CRIAÇÃO / EDIÇÃO) */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white w-full max-w-lg rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in-95 max-h-[90vh] overflow-y-auto">
            <div className="p-6 bg-slate-900 text-white flex justify-between items-center sticky top-0 z-10">
                <h3 className="font-bold text-lg flex items-center gap-2">
                    {editingOrderId ? <Edit className="text-emerald-400"/> : <Package className="text-emerald-400"/>} 
                    {editingOrderId ? 'Editar Encomenda' : 'Registrar Encomenda'}
                </h3>
                <button onClick={() => setIsModalOpen(false)}><X className="text-slate-400 hover:text-white"/></button>
            </div>
            
            <form onSubmit={handleSaveOrder} className="p-6 space-y-6">
                
                {/* 1. LOCALIZAR E DADOS DO MORADOR */}
                <div className="space-y-4">
                    <div className="flex justify-between items-end">
                       <label className="text-xs font-black text-slate-400 uppercase">1. Dados do Morador</label>
                       {formData.residentId && <span className="text-[10px] bg-emerald-100 text-emerald-700 px-2 py-0.5 rounded font-bold">Localizado</span>}
                    </div>
                    
                    {/* Barra de Busca - Exibe apenas se NÃO estiver editando ou se quiser permitir troca */}
                    <div className="relative">
                        <input 
                            type="text" 
                            className="w-full p-3 pl-10 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white focus:ring-2 focus:ring-emerald-500 outline-none font-bold text-slate-700 placeholder:font-normal"
                            placeholder="Pesquisar por Nome, Bloco ou Unidade..."
                            value={residentSearch}
                            onChange={e => setResidentSearch(e.target.value)}
                        />
                        <Search className="absolute left-3 top-3.5 text-slate-400" size={18}/>
                        
                        {residentSearch && (
                            <div className="absolute w-full mt-1 bg-white border border-slate-200 rounded-xl shadow-xl max-h-48 overflow-y-auto z-50">
                                {filteredResidents.length === 0 ? (
                                    <div className="p-3 text-sm text-slate-400 flex items-center gap-2"><SearchX size={14}/> Sem resultados</div>
                                ) : filteredResidents.map(r => (
                                    <div key={r.id} onClick={() => handleSelectResident(r)} className="p-3 hover:bg-emerald-50 cursor-pointer border-b border-slate-50 last:border-0">
                                        <p className="font-bold text-slate-700 text-sm">{r.nome}</p>
                                        <p className="text-xs text-slate-500">Bl {r.bloco} - Apto {r.unidade}</p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="grid grid-cols-1 gap-3 animate-in fade-in slide-in-from-top-2">
                        <div className="space-y-1">
                             <label className="text-[10px] font-bold text-slate-500 uppercase ml-1 flex items-center gap-1"><User size={10}/> Nome do Titular</label>
                             <input readOnly className="w-full p-3 border border-slate-200 rounded-xl text-slate-600 bg-slate-50 text-sm outline-none" value={formData.residentName} placeholder="Selecione na busca acima" />
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                             <div className="space-y-1">
                                <label className="text-[10px] font-bold text-slate-500 uppercase ml-1 flex items-center gap-1"><Building size={10}/> Bloco</label>
                                <input readOnly className="w-full p-3 border border-slate-200 rounded-xl text-slate-600 bg-slate-50 text-sm outline-none" value={formData.block} placeholder="-" />
                             </div>
                             <div className="space-y-1">
                                <label className="text-[10px] font-bold text-slate-500 uppercase ml-1 flex items-center gap-1"><MapPin size={10}/> Unidade</label>
                                <input readOnly className="w-full p-3 border border-slate-200 rounded-xl text-slate-600 bg-slate-50 text-sm outline-none" value={formData.unit} placeholder="-" />
                             </div>
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                             <div className="space-y-1">
                                <label className="text-[10px] font-bold text-slate-500 uppercase ml-1 flex items-center gap-1"><AtSign size={10}/> E-mail</label>
                                <input readOnly className="w-full p-3 border border-slate-200 rounded-xl text-slate-600 bg-slate-50 text-sm outline-none truncate" value={formData.email} placeholder="-" />
                             </div>
                             <div className="space-y-1">
                                <label className="text-[10px] font-bold text-slate-500 uppercase ml-1 flex items-center gap-1"><Phone size={10}/> WhatsApp</label>
                                <input readOnly className="w-full p-3 border border-slate-200 rounded-xl text-slate-600 bg-slate-50 text-sm outline-none" value={formData.whatsapp} placeholder="-" />
                             </div>
                        </div>
                    </div>
                </div>

                {/* 2. DADOS DO PACOTE */}
                <div className="space-y-3 pt-4 border-t border-slate-100">
                    <label className="text-xs font-black text-slate-400 uppercase">2. Dados do Pacote</label>
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1">
                            <label className="text-[10px] font-bold text-slate-500 uppercase ml-1">Código Rastreio</label>
                            <input required className="w-full p-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500" placeholder="Ex: BR123456" value={formData.trackingCode} onChange={e => setFormData({...formData, trackingCode: e.target.value})} />
                        </div>
                        <div className="space-y-1">
                            <label className="text-[10px] font-bold text-slate-500 uppercase ml-1">Origem</label>
                            <input className="w-full p-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500" placeholder="Ex: Amazon, Shopee" value={formData.origin} onChange={e => setFormData({...formData, origin: e.target.value})} />
                        </div>
                    </div>
                    <div className="space-y-1">
                        <label className="text-[10px] font-bold text-slate-500 uppercase ml-1">Nome na Etiqueta</label>
                        <input required className="w-full p-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500" placeholder="Nome escrito na caixa" value={formData.recipientName} onChange={e => setFormData({...formData, recipientName: e.target.value})} />
                    </div>
                    <div className="space-y-1">
                        <label className="text-[10px] font-bold text-slate-500 uppercase ml-1">Data de Chegada</label>
                        <input type="datetime-local" className="w-full p-3 border border-slate-200 rounded-xl text-sm outline-none focus:border-emerald-500 text-slate-600" value={formData.arrivalDate} onChange={e => setFormData({...formData, arrivalDate: e.target.value})} />
                    </div>
                </div>

                <button disabled={isLoading} type="submit" className="w-full bg-emerald-600 text-white p-4 rounded-xl font-bold hover:bg-emerald-700 shadow-lg shadow-emerald-200 transition-all disabled:opacity-50 mt-4 flex items-center justify-center gap-2">
                    {isLoading ? 'Salvando...' : (editingOrderId ? <><Save size={18}/> Salvar Alterações</> : 'Confirmar Recebimento')}
                </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Orders;
import React, { useState, useMemo, useEffect, useRef } from 'react';
import { 
  Search, Plus, Clock, Trash2, X, SearchX, CheckCircle, 
  Edit, Save, QrCode, UserCheck, UserPlus, 
  CalendarDays, Share2, Phone, Mail, FileText
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import QRCode from 'react-qr-code';
import html2canvas from 'html2canvas';

interface Resident {
  id: string;
  nome: string;
  email: string;
  cpf?: string;
  whatsapp?: string;
  phone?: string; 
  unidade?: string;
  bloco?: string;
}

interface Guest {
  id: string;
  guestName: string;
  guestRg: string;
  scheduledDate?: string; 
  accessCode: string; 
  status: 'PENDING' | 'AUTHORIZED' | 'CANCELED'; 
  entryTime?: string;
  residentId: string;
  residentName: string;
  unit: string;
  block: string;
  residentWhatsapp?: string;
  residentCpf?: string;
  residentEmail?: string;
}

const Guests: React.FC = () => {
  const { user } = useAuth();
  
  // VERIFICAÇÃO INFALÍVEL DE CARGO
  const userRole = String(user?.role || '').toUpperCase();
  const isStaff = ['SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO', 'ADMIN'].some(r => userRole.includes(r));

  const [guests, setGuests] = useState<Guest[]>([]);
  const [residentsList, setResidentsList] = useState<Resident[]>([]);
  const [activeTab, setActiveTab] = useState<'PENDING' | 'AUTHORIZED'>('PENDING');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isQrModalOpen, setIsQrModalOpen] = useState<Guest | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [residentSearch, setResidentSearch] = useState('');
  const [globalSearch, setGlobalSearch] = useState('');
  const [editingGuestId, setEditingGuestId] = useState<string | null>(null);
  const qrRef = useRef<HTMLDivElement>(null);

  const [formData, setFormData] = useState({
    residentId: '', residentName: '', block: '', unit: '', whatsapp: '', cpf: '', email: '',
    guestName: '', guestRg: '', scheduledDate: ''
  });

  // ================= MÁSCARAS DE FORMATAÇÃO =================
  const formatRG = (value: string | undefined) => {
    if (!value) return '';
    const rg = String(value).replace(/\D/g, ''); 
    return rg
      .replace(/(\d{2})(\d)/, '$1.$2') 
      .replace(/(\d{3})(\d)/, '$1.$2') 
      .replace(/(\d{3})(\d{1,2})$/, '$1-$2') 
      .substring(0, 13); 
  };

  const formatCPF = (value: string | undefined) => {
    if (!value) return ''; 
    let cpf = String(value).replace(/\D/g, '');
    if (cpf.length > 11) cpf = cpf.substring(0, 11);
    if (cpf.length === 11) {
        return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
    }
    return cpf;
  };

  const formatPhone = (value: string | undefined) => {
    if (!value) return ''; 
    let phone = String(value).replace(/\D/g, '');
    if (phone.length > 11) phone = phone.substring(0, 11);
    if (phone.length === 11) {
        return phone.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3");
    } else if (phone.length === 10) {
        return phone.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3");
    }
    return phone;
  };

  // Formata data para o padrão exato da imagem: 03/03/2026, 07:28
  const formatDateStr = (dateString: string) => {
    const d = new Date(dateString);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year}, ${hours}:${minutes}`;
  };
  // =========================================================

  useEffect(() => { loadData(); }, [user]);

  const loadData = async () => {
    if (!user) return;
    try {
        setIsLoading(true);
        const resGuests = await api.get('/guests');
        setGuests(resGuests.data || []);
        
        if (isStaff) {
           const resUsers = await api.get('/users');
           setResidentsList(resUsers.data.filter((u: any) => u.role !== 'ADMIN' && u.nome));
        }
    } catch (error) {
        console.error("Erro ao carregar dados", error);
    } finally {
        setIsLoading(false);
    }
  };

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
    const rawPhone = resident.whatsapp || resident.phone || (resident as any).telefone || (resident as any).celular || '';
    
    setFormData(prev => ({
      ...prev, 
      residentId: resident.id, 
      residentName: resident.nome,
      block: resident.bloco || 'N/A', 
      unit: resident.unidade || 'N/A',
      whatsapp: formatPhone(rawPhone), 
      cpf: formatCPF(resident.cpf || ''), 
      email: resident.email || ''
    }));
    setResidentSearch('');
  };

  const displayedGuests = guests.filter(g => {
    const matchTab = activeTab === 'AUTHORIZED' ? g.status === 'AUTHORIZED' : g.status === 'PENDING';
    if (!matchTab) return false;
    if (globalSearch) {
        const term = globalSearch.toLowerCase();
        return g.guestName.toLowerCase().includes(term) || g.guestRg.toLowerCase().includes(term) || g.residentName.toLowerCase().includes(term);
    }
    return true;
  });

  const openCreateModal = () => {
    setEditingGuestId(null);
    if (isStaff) {
      setFormData({ residentId: '', residentName: '', block: '', unit: '', whatsapp: '', cpf: '', email: '', guestName: '', guestRg: '', scheduledDate: '' });
    } else {
      const userPhone = user?.whatsapp || (user as any)?.phone || (user as any)?.telefone || (user as any)?.celular || '';
      
      setFormData({
        residentId: user?.id || '',
        residentName: user?.nome || '',
        block: user?.bloco || 'N/A',
        unit: user?.unidade || 'N/A',
        whatsapp: formatPhone(userPhone), 
        cpf: formatCPF(user?.cpf),
        email: user?.email || '',
        guestName: '', guestRg: '', scheduledDate: ''
      });
    }
    setIsModalOpen(true);
  };

  const openEditModal = (guest: Guest) => {
    setEditingGuestId(guest.id);
    setFormData({ 
        residentId: guest.residentId, 
        residentName: guest.residentName, 
        block: guest.block, 
        unit: guest.unit, 
        whatsapp: formatPhone(guest.residentWhatsapp), 
        cpf: formatCPF(guest.residentCpf), 
        email: guest.residentEmail || '', 
        guestName: guest.guestName, 
        guestRg: guest.guestRg, 
        scheduledDate: guest.scheduledDate ? guest.scheduledDate.substring(0, 16) : '' 
    });
    setIsModalOpen(true);
  };

  const handleSaveGuest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.residentId) return alert("Erro: Morador responsável não identificado.");
    try {
      setIsLoading(true);
      
      const cleanCpf = formData.cpf ? formData.cpf.replace(/\D/g, '') : '';
      const cleanPhone = formData.whatsapp ? formData.whatsapp.replace(/\D/g, '') : '';

      const payload = {
        guestName: formData.guestName, 
        guestRg: formData.guestRg, 
        scheduledDate: formData.scheduledDate ? new Date(formData.scheduledDate).toISOString() : null,
        residentId: formData.residentId, 
        residentName: formData.residentName,
        unit: formData.unit, 
        block: formData.block, 
        residentWhatsapp: cleanPhone, 
        residentCpf: cleanCpf,        
        residentEmail: formData.email
      };
      
      let res: { data: Guest };
      if (editingGuestId) {
          res = await api.put(`/guests/${editingGuestId}`, payload);
          setGuests(prev => prev.map(g => g.id === editingGuestId ? res.data : g));
      } else {
          res = await api.post('/guests', payload);
          setGuests([res.data, ...guests]); 
      }
      setIsModalOpen(false);
    } catch (error) {
      alert("Erro ao salvar.");
    } finally { setIsLoading(false); }
  };

  const handleManualEntry = async (guestId: string) => {
    if (!confirm("Confirmar entrada manual?")) return;
    try {
        const res = await api.put(`/guests/${guestId}/authorize`); 
        setGuests(prev => prev.map(g => g.id === guestId ? res.data : g));
    } catch (error) { alert("Erro ao autorizar."); }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Tem certeza que deseja excluir este convite?")) return;
    try {
        await api.delete(`/guests/${id}`);
        setGuests(prev => prev.filter(g => g.id !== id));
    } catch (e) { alert("Erro ao excluir. Verifique se o servidor está rodando."); }
  };

  // URL DE VALIDAÇÃO FIXADA PARA O DOMÍNIO DE PRODUÇÃO
  const getQrCodeUrl = (token: string) => `https://votzz.com.br/portaria/validar?token=${token}`;

  const handleShareWhatsApp = async () => {
      if (!qrRef.current || !isQrModalOpen) return;
      try {
          const canvas = await html2canvas(qrRef.current, { backgroundColor: '#ffffff', scale: 2 });
          canvas.toBlob(async (blob) => {
              if (!blob) return;
              const file = new File([blob], `Convite-${isQrModalOpen.guestName.replace(/\s+/g, '-')}.png`, { type: 'image/png' });
              
              let dateTxt = '';
              if (isQrModalOpen.scheduledDate) {
                  dateTxt = `\n📅 Agendado: *${formatDateStr(isQrModalOpen.scheduledDate)}*`;
              }

              const text = `Olá, *${isQrModalOpen.guestName}*! Seu acesso ao condomínio foi autorizado por *${isQrModalOpen.residentName}* (Apto ${isQrModalOpen.unit}).${dateTxt}\n\nApresente o QR Code na portaria.\nOu use o link:\n🔗 ${getQrCodeUrl(isQrModalOpen.accessCode)}`;
              
              if (navigator.canShare && navigator.canShare({ files: [file] })) {
                  await navigator.share({ files: [file], title: 'Convite Votzz', text });
              } else {
                  const item = new ClipboardItem({ "image/png": blob });
                  await navigator.clipboard.write([item]);
                  alert("QR Code copiado! Cole no WhatsApp.");
                  window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank');
              }
          }, 'image/png');
      } catch (e) { alert("Erro ao gerar convite."); }
  };

  return (
    <div className="p-6 md:p-8 bg-slate-50 min-h-screen">
      
      {/* HEADER */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-800 flex items-center gap-3">
            <UserCheck className="text-emerald-600" size={32} /> Controle de Convidados
          </h1>
          <p className="text-slate-500 mt-1">Geração de QR Codes e autorização de portaria.</p>
        </div>
        <div className="flex w-full lg:w-auto gap-3">
            {guests.length > 0 && isStaff && (
                <div className="relative flex-1 lg:w-80">
                    <input type="text" placeholder="Pesquisar..." className="w-full p-3 pl-10 border border-slate-200 rounded-xl outline-none focus:border-emerald-500" value={globalSearch} onChange={e => setGlobalSearch(e.target.value)} />
                    <Search className="absolute left-3 top-3.5 text-slate-400" size={18}/>
                </div>
            )}
            <button onClick={openCreateModal} className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-3 rounded-xl font-bold flex items-center gap-2 transition-all shadow-lg">
                <Plus size={20} /> Novo
            </button>
        </div>
      </div>

      {/* TABS */}
      <div className="flex gap-6 mb-6 border-b border-slate-200">
        <button onClick={() => setActiveTab('PENDING')} className={`pb-3 px-2 font-bold flex items-center gap-2 ${activeTab === 'PENDING' ? 'border-b-4 border-emerald-500 text-emerald-700' : 'text-slate-400'}`}>
          <Clock size={18} /> Aguardando Entrada
        </button>
        <button onClick={() => setActiveTab('AUTHORIZED')} className={`pb-3 px-2 font-bold flex items-center gap-2 ${activeTab === 'AUTHORIZED' ? 'border-b-4 border-emerald-500 text-emerald-700' : 'text-slate-400'}`}>
          <CheckCircle size={18} /> Já Entraram
        </button>
      </div>

      {/* LISTAGEM DE CARDS */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {displayedGuests.length === 0 && (
            <div className="col-span-full py-20 text-center text-slate-400 bg-white rounded-3xl border border-slate-100 border-dashed">
                <UserCheck size={48} className="mx-auto mb-3 opacity-20" />
                <p>Nenhum convidado encontrado.</p>
            </div>
        )}

        {displayedGuests.map(guest => {
          const wppStr = guest.residentWhatsapp ? String(guest.residentWhatsapp).replace(/\D/g, '') : '';
          const cpfStr = guest.residentCpf ? String(guest.residentCpf).replace(/\D/g, '') : '';
          
          return (
          <div key={guest.id} className="bg-white rounded-2xl shadow-sm border border-slate-200 flex flex-col overflow-hidden relative hover:shadow-md transition-shadow">
            
            {/* Ícones de Edição */}
            {guest.status === 'PENDING' && (
                <div className="absolute top-4 right-4 flex gap-1">
                    <button onClick={() => openEditModal(guest)} className="text-slate-400 hover:text-blue-500 p-1"><Edit size={16} /></button>
                    <button onClick={() => handleDelete(guest.id)} className="text-slate-400 hover:text-red-500 p-1"><Trash2 size={16} /></button>
                    <button onClick={() => setIsQrModalOpen(guest)} className="text-emerald-600 p-1 bg-emerald-50 rounded ml-1"><QrCode size={18} /></button>
                </div>
            )}
            
            <div className="p-6 flex flex-col h-full">
                
                {/* NOME E RG DO VISITANTE */}
                <div className="mb-4 pr-20">
                    <h3 className="font-bold text-xl text-slate-800 leading-tight uppercase tracking-tight">{guest.guestName}</h3>
                    <p className="text-slate-500 text-sm font-semibold mt-0.5">RG: {formatRG(guest.guestRg)}</p>
                </div>
                
                {/* DATA AGENDADA */}
                {guest.scheduledDate && guest.status === 'PENDING' && (
                    <div className="mb-5 bg-orange-50 text-orange-800 p-3 rounded-xl text-sm font-bold border border-orange-100 flex items-center gap-2">
                        Agendado: {formatDateStr(guest.scheduledDate)}
                    </div>
                )}
                
                {/* DADOS DO MORADOR AUTORIZANTE */}
                <div className="mb-6 flex-1 space-y-2">
                    <div>
                      <p className="text-[10px] text-slate-400 uppercase font-bold tracking-widest mb-1">Autorizado por</p>
                      <p className="font-bold text-slate-800 text-base">{guest.residentName}</p>
                    </div>
                    
                    <div className="flex items-center gap-2 text-sm text-slate-500 font-medium mt-1 mb-3">
                      <span>Bl {guest.block || '-'}</span>
                      <span className="text-slate-300">|</span>
                      <span>Ap {guest.unit || '-'}</span>
                    </div>
                    
                    <div className="space-y-2 pt-4 mt-2 border-t border-slate-100 text-sm text-slate-600">
                      <p className="font-mono flex items-center gap-3">
                         <Phone size={14} className="text-slate-400 shrink-0"/> 
                         {wppStr.length >= 10 ? formatPhone(wppStr) : 'Não informado'}
                      </p>
                      <p className="font-mono flex items-center gap-3">
                         <FileText size={14} className="text-slate-400 shrink-0"/> 
                         {cpfStr.length >= 11 ? formatCPF(cpfStr) : 'Não informado'}
                      </p>
                      <p className="truncate flex items-center gap-3" title={guest.residentEmail}>
                         <Mail size={14} className="text-slate-400 shrink-0"/> 
                         {guest.residentEmail || 'Não informado'}
                      </p>
                    </div>
                </div>
                
                {/* Botão Base */}
                <div className="mt-auto">
                    {guest.status === 'AUTHORIZED' ? (
                        <div className="flex justify-between items-center p-3 rounded-xl border bg-emerald-50 border-emerald-200 text-emerald-800">
                            <span className="font-bold">Entrada Confirmada</span>
                            <span className="font-mono text-xs">{guest.entryTime ? formatDateStr(guest.entryTime) : '--:--'}</span>
                        </div>
                    ) : (isStaff && <button onClick={() => handleManualEntry(guest.id)} className="w-full bg-slate-900 text-white p-3 rounded-xl font-bold hover:bg-black transition-colors">Autorizar Manualmente</button>)}
                </div>
            </div>
          </div>
        )})}
      </div>

      {/* MODAL DE CADASTRO/EDIÇÃO */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white w-full max-w-xl rounded-3xl overflow-hidden shadow-2xl">
            <div className="p-6 bg-slate-900 text-white flex justify-between">
                <h3 className="font-bold flex items-center gap-2">{editingGuestId ? <Edit size={18}/> : <UserPlus size={18}/>} {editingGuestId ? 'Editar Convite' : 'Novo Convite'}</h3>
                <button onClick={() => setIsModalOpen(false)}><X/></button>
            </div>
            <form onSubmit={handleSaveGuest} className="p-6 space-y-4">
                
                {/* BUSCA DE MORADOR (SÓ PARA STAFF) */}
                {isStaff && (
                    <div className="relative">
                        <label className="text-xs font-bold text-slate-400 uppercase">1. Morador Responsável</label>
                        <input type="text" className="w-full p-3 border rounded-xl mt-1 focus:ring-2 focus:ring-emerald-500 outline-none" placeholder="Pesquisar morador (Apenas Gestão)..." value={residentSearch} onChange={e => setResidentSearch(e.target.value)} />
                        {residentSearch && (
                            <div className="absolute w-full mt-1 bg-white border rounded-xl shadow-xl z-50 max-h-40 overflow-auto">
                                {filteredResidents.map(r => (
                                    <div key={r.id} onClick={() => handleSelectResident(r)} className="p-3 hover:bg-emerald-50 cursor-pointer border-b last:border-0">
                                        <p className="font-bold text-sm text-slate-700">{r.nome} <span className="text-slate-400 font-normal">(Bl {r.bloco} - Ap {r.unidade})</span></p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* DADOS DO MORADOR (CAIXAS DE INPUT VISÍVEIS) */}
                {formData.residentName && (
                  <div className="p-5 bg-slate-50 rounded-2xl border border-slate-200 mt-2">
                    <div className="flex justify-between items-start mb-4 border-b border-slate-200 pb-3">
                      <div>
                        <p className="text-[10px] font-bold text-slate-400 uppercase">Morador Autorizante</p>
                        <p className="text-sm font-bold text-slate-800">{formData.residentName}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-[10px] font-bold text-slate-400 uppercase">Localização</p>
                        <p className="text-sm font-bold text-slate-600">Bl {formData.block} / Ap {formData.unit}</p>
                      </div>
                    </div>
                    
                    <p className="text-xs text-slate-500 font-medium mb-3">Confirme ou atualize os dados do morador para o registro da portaria:</p>
                    
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="text-[10px] font-bold text-slate-500 uppercase mb-1 block">CPF *</label>
                        <input 
                          type="text"
                          required
                          className="w-full p-2.5 text-xs text-slate-800 font-bold bg-white border border-slate-300 rounded-lg outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 font-mono transition-all"
                          placeholder="000.000.000-00"
                          value={formData.cpf}
                          onChange={e => setFormData({...formData, cpf: formatCPF(e.target.value)})}
                        />
                      </div>
                      <div>
                        <label className="text-[10px] font-bold text-slate-500 uppercase mb-1 block">WhatsApp *</label>
                        <input 
                          type="text"
                          required
                          className="w-full p-2.5 text-xs text-slate-800 font-bold bg-white border border-slate-300 rounded-lg outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 font-mono transition-all"
                          placeholder="(00) 00000-0000"
                          value={formData.whatsapp}
                          onChange={e => setFormData({...formData, whatsapp: formatPhone(e.target.value)})}
                        />
                      </div>
                      <div className="col-span-2">
                        <label className="text-[10px] font-bold text-slate-500 uppercase mb-1 block">E-mail</label>
                        <input 
                          type="email"
                          className="w-full p-2.5 text-xs text-slate-800 font-bold bg-white border border-slate-300 rounded-lg outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all"
                          placeholder="email@condominio.com"
                          value={formData.email}
                          onChange={e => setFormData({...formData, email: e.target.value})}
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* DADOS DA VISITA */}
                <div className="border-t border-slate-200 pt-5 mt-2 space-y-3">
                    <label className="text-xs font-bold text-slate-400 uppercase">2. Dados da Visita</label>
                    <input type="datetime-local" className="w-full p-3 border border-slate-300 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" value={formData.scheduledDate} onChange={e => setFormData({...formData, scheduledDate: e.target.value})} />
                    <div className="grid grid-cols-2 gap-3">
                        <input required className="p-3 border border-slate-300 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" placeholder="Nome do Visitante" value={formData.guestName} onChange={e => setFormData({...formData, guestName: e.target.value})} />
                        
                        <input 
                          required 
                          className="p-3 border border-slate-300 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500 font-mono" 
                          placeholder="RG do Visitante" 
                          maxLength={13} 
                          value={formData.guestRg} 
                          onChange={e => setFormData({...formData, guestRg: formatRG(e.target.value)})} 
                        />
                    </div>
                </div>
                <button type="submit" disabled={isLoading} className="w-full bg-emerald-600 text-white p-4 rounded-xl font-bold flex justify-center gap-2 transition-colors hover:bg-emerald-700 mt-2">
                    <Save size={18}/> {isLoading ? 'Salvando...' : 'Salvar Convite'}
                </button>
            </form>
          </div>
        </div>
      )}

      {/* MODAL QR CODE (PASSE) */}
      {isQrModalOpen && (
          <div className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-4">
            <div className="bg-white w-full max-w-sm rounded-3xl text-center relative animate-in zoom-in-95 overflow-hidden">
                <button onClick={() => setIsQrModalOpen(null)} className="absolute top-4 right-4"><X/></button>
                <div ref={qrRef} className="p-8 bg-white flex flex-col items-center">
                    <h3 className="font-black text-2xl mb-1 uppercase text-slate-900 tracking-tighter">Passe de Acesso</h3>
                    <p className="text-slate-500 font-bold mb-8 text-lg">{isQrModalOpen.guestName}</p>
                    <div className="bg-white p-4 rounded-3xl border-4 border-slate-900 mb-6 shadow-xl">
                        <QRCode value={getQrCodeUrl(isQrModalOpen.accessCode)} size={200} level="H" />
                    </div>
                    <div className="text-center">
                         <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mb-1">Autorizado por</p>
                         <p className="text-sm font-bold text-slate-800">{isQrModalOpen.residentName}</p>
                         <p className="text-xs text-slate-500 font-medium">Bl {isQrModalOpen.block} - Ap {isQrModalOpen.unit}</p>
                    </div>
                </div>
                <div className="p-6 bg-slate-50 border-t">
                    <button onClick={handleShareWhatsApp} className="w-full bg-green-500 text-white p-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-green-600 transition-colors shadow-lg shadow-green-100">
                        <Share2 size={18}/> Compartilhar Agora
                    </button>
                </div>
            </div>
          </div>
      )}
    </div>
  );
};

export default Guests;
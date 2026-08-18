import React, { useState, useEffect, useMemo } from 'react';
import { 
  CalendarDays, 
  MapPin, 
  Users, 
  Clock, 
  Plus, 
  CheckCircle, 
  XCircle, 
  AlertCircle, 
  Wallet, 
  Edit, 
  Save, 
  Trash2, 
  Search, 
  User as UserIcon, 
  Upload, 
  ImageIcon, 
  Shield, 
  ArrowRight, 
  FileText, 
  Banknote, 
  Copy, 
  ExternalLink, 
  ThumbsUp, 
  ThumbsDown, 
  CreditCard, 
  Landmark, 
  AlertTriangle, 
  Timer, 
  Lock, 
  Unlock, 
  Eye, 
  EyeOff, 
  Info, 
  CalendarCheck, 
  DollarSign, 
  QrCode, 
  Image as ImageIcon2, 
  Archive, 
  History, 
  ChevronDown, 
  ChevronUp, 
  LayoutDashboard, 
  Settings, 
  ClipboardCheck, 
  Building2, 
  UserCheck, 
  Zap, 
  Activity, 
  Server, 
  Layers, 
  Database, 
  BarChart3, 
  RefreshCw, 
  Cpu, 
  Globe, 
  HardDrive,
  Ban
} from 'lucide-react';
import api from '../services/api'; 
import { CommonArea, Booking, User, BookingStatus } from '../types';
import { useAuth } from '../context/AuthContext';

// ============================================================================
// SEÇÃO 1: EXTENSÃO DE INTERFACES E TIPOS (PROTOCOLO VOTZZ)
// ============================================================================

/**
 * Interface estendida para suportar propriedades dinâmicas do Backend.
 * Inclui suporte a snake_case e objetos aninhados do JPA.
 */
type ExtendedBooking = Booking & {
  receiptUrl?: string;
  receipt_url?: string; // Fallback snake_case
  comprovante?: string;
  unidade?: string;
  unit?: string;
  nome?: string;
  // Campos auxiliares para governança
  block?: string;
  whatsapp?: string;
  cpf?: string;
  
  // Campos vindos do Backend Java (Nested Objects)
  bookingDate?: string; 
  commonArea?: {
      id: string;
      name: string;
      price?: number;
      imageUrl?: string;
  };
  user?: {
      id: string;
      nome?: string;
      name?: string;
      email?: string;
  };
  
  // Fallbacks de IDs planos e timestamps
  areaId?: string;
  userId?: string;
  createdAt?: string;
  created_at?: string; // Correção TS2551
};

interface CondoPaymentConfig {
  enableAsaas: boolean;
  enableManualPix: boolean;
  bankName: string;
  agency: string;
  account: string;
  pixKey: string;
  instructions: string;
  asaasAccessToken?: string; 
}

interface BookingFormData {
  date: string;
  startTime: string;
  endTime: string;
  nome: string;
  cpf: string;
  whatsapp: string;
  bloco: string;
  unidade: string;
}

interface PaymentResponseData {
  bookingId: string;
  billingType: string;
  pixCopyPaste?: string;
  invoiceUrl?: string;
  qrCodeImage?: string;
  value: number;
}

// ============================================================================
// SEÇÃO 2: COMPONENTES AUXILIARES DE ALTA DISPONIBILIDADE
// ============================================================================

/**
 * Componente Timer: Gerencia visualmente o tempo restante para pagamento.
 * Se o tempo expirar, dispara callback para recarregar dados.
 */
const BookingTimer: React.FC<{ createdAt?: string; onExpire: () => void }> = ({ createdAt, onExpire }) => {
    const [timeLeft, setTimeLeft] = useState<string>("");
    const [isExpired, setIsExpired] = useState(false);

    useEffect(() => {
        if (!createdAt) return;

        const interval = setInterval(() => {
            const created = new Date(createdAt).getTime();
            const now = new Date().getTime();
            // Janela de 30 minutos para pagamento
            const deadline = created + (30 * 60 * 1000); 
            const distance = deadline - now;

            if (distance < 0) {
                clearInterval(interval);
                setTimeLeft("EXPIRADO");
                if (!isExpired) {
                    setIsExpired(true);
                    onExpire(); 
                }
            } else {
                const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
                const seconds = Math.floor((distance % (1000 * 60)) / 1000);
                const m = minutes < 10 ? `0${minutes}` : minutes;
                const s = seconds < 10 ? `0${seconds}` : seconds;
                setTimeLeft(`${m}m ${s}s`);
            }
        }, 1000);

        return () => clearInterval(interval);
    }, [createdAt, onExpire, isExpired]);

    if (!createdAt) return null;

    if (timeLeft === "EXPIRADO") {
        return (
            <span className="font-mono text-slate-400 font-bold text-[10px] bg-slate-100 px-2 py-1 rounded-lg border border-slate-200 flex items-center gap-1.5 uppercase">
                <Ban size={12} /> TEMPO ESGOTADO
            </span>
        );
    }

    return (
        <span className="font-mono text-red-600 font-black text-[10px] bg-red-50 px-2 py-1 rounded-lg border border-red-100 flex items-center gap-1.5 animate-pulse uppercase">
            <Timer size={12} />
            {timeLeft}
        </span>
    );
};

const PaymentModal: React.FC<{ 
    isOpen: boolean; 
    onClose: () => void; 
    onGoToMyBookings: () => void;
    data: PaymentResponseData | null;
    config: CondoPaymentConfig;
}> = ({ isOpen, onClose, onGoToMyBookings, data, config }) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/80 backdrop-blur-md p-4 animate-in fade-in duration-300">
            <div className="bg-white rounded-[40px] shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh] border border-white/20">
                <div className="bg-slate-900 p-8 text-white flex justify-between items-center relative">
                    <div className="absolute top-0 right-0 p-2 opacity-10">
                        <DollarSign size={80}/>
                    </div>
                    <div className="relative z-10">
                        <h3 className="text-2xl font-black tracking-tighter flex items-center gap-3">
                            <Banknote className="text-emerald-400" size={28}/> Pagamento Pendente
                        </h3>
                        <p className="text-slate-400 text-xs mt-2 font-bold uppercase tracking-widest">Sua reserva foi pré-agendada no motor Votzz</p>
                    </div>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors bg-white/10 p-3 rounded-2xl relative z-10">
                        <XCircle size={24} />
                    </button>
                </div>

                <div className="p-8 overflow-y-auto space-y-8">
                    <div className="bg-amber-50 border-2 border-amber-100 rounded-[24px] p-5 flex items-start gap-4 shadow-sm">
                        <div className="bg-amber-500 p-2 rounded-xl text-white">
                            <Timer size={24}/>
                        </div>
                        <div>
                            <p className="text-sm font-black text-amber-900 uppercase">Janela de Validação: 30 Minutos!</p>
                            <p className="text-xs text-amber-700 font-medium mt-1 leading-relaxed">
                                Realize a transferência e envie o comprovante agora para garantir a posse da data. Após esse prazo, o sistema libera a data automaticamente.
                            </p>
                        </div>
                    </div>

                    <div className="bg-slate-50 p-6 rounded-[32px] border border-slate-200 shadow-inner">
                        <h4 className="font-black text-slate-800 text-xs mb-5 flex items-center gap-2 uppercase tracking-[2px] border-b pb-4 border-slate-200">
                            <Landmark size={18} className="text-slate-400"/> Dados para Transferência Bancária
                        </h4>
                        <div className="space-y-4 text-sm text-slate-600">
                            <div className="flex justify-between items-center bg-white p-4 rounded-2xl border border-slate-100">
                                <span className="text-[10px] font-black text-slate-400 uppercase">Banco</span> 
                                <span className="font-black text-slate-900 uppercase tracking-tighter">{config.bankName || "Não informado"}</span>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="bg-white p-4 rounded-2xl border border-slate-100">
                                    <span className="text-[10px] font-black text-slate-400 uppercase block mb-1">Agência</span> 
                                    <span className="font-black text-slate-900">{config.agency || "---"}</span>
                                </div>
                                <div className="bg-white p-4 rounded-2xl border border-slate-100">
                                    <span className="text-[10px] font-black text-slate-400 uppercase block mb-1">Conta Corrente</span> 
                                    <span className="font-black text-slate-900">{config.account || "---"}</span>
                                </div>
                            </div>
                            <div className="pt-4">
                                <p className="text-[10px] font-black text-slate-500 uppercase mb-2 tracking-widest ml-1 text-center">Chave Pix Identificada</p>
                                <div className="flex gap-3 items-center bg-slate-900 p-5 rounded-2xl border border-slate-800 shadow-xl">
                                    <code className="flex-1 text-xs font-mono text-emerald-400 break-all font-bold tracking-tight">{config.pixKey || "---"}</code>
                                    <button 
                                        onClick={() => { navigator.clipboard.writeText(config.pixKey); alert("CHAVE COPIADA: Cole no seu aplicativo bancário."); }} 
                                        className="bg-emerald-500 text-white p-3 rounded-xl hover:bg-emerald-400 transition-all shadow-lg active:scale-90"
                                        title="Copiar Chave"
                                    >
                                        <Copy size={20}/>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <button 
                        onClick={onGoToMyBookings} 
                        className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-5 rounded-[24px] font-black text-xs shadow-2xl transition-all flex items-center justify-center gap-3 uppercase tracking-[2px] group"
                    >
                        <ImageIcon2 size={20}/> Ir para Minhas Reservas e Anexar <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform"/>
                    </button>
                </div>
                
                <div className="bg-slate-50 p-6 border-t border-slate-100 text-center">
                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Tecnologia Votzz © 2026 - Gestão de Pagamentos</p>
                </div>
            </div>
        </div>
    );
};

// ============================================================================
// COMPONENTE PRINCIPAL: SPACES (SISTEMA INTEGRADO)
// ============================================================================

const Spaces: React.FC = () => {
  const { user } = useAuth();
  
  // ESTADOS DE NEGÓCIO
  const [areas, setAreas] = useState<CommonArea[]>([]);
  const [bookings, setBookings] = useState<ExtendedBooking[]>([]); 
  const [selectedArea, setSelectedArea] = useState<CommonArea | null>(null);
  
  // ESTADOS DE NAVEGAÇÃO E TABS
  const [activeTab, setActiveTab] = useState<'areas' | 'my-bookings' | 'manage'>('areas');
  const [manageSubTab, setManageSubTab] = useState<'calendar' | 'config' | 'validations'>('calendar');
  
  // ESTADOS DE CONFIGURAÇÃO FINANCEIRA
  const [paymentConfig, setPaymentConfig] = useState<CondoPaymentConfig>({
    enableAsaas: false,
    enableManualPix: true, 
    bankName: '',
    agency: '',
    account: '',
    pixKey: '',
    instructions: '',
    asaasAccessToken: ''
  });

  // ESTADOS DE CONTROLE DE INTERFACE
  const [isEditingConfig, setIsEditingConfig] = useState(false);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [lastPaymentData, setLastPaymentData] = useState<PaymentResponseData | null>(null);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [uploadingBookingId, setUploadingBookingId] = useState<string | null>(null);
  const [showArchived, setShowArchived] = useState(false);

  // ESTADOS DE FORMULÁRIOS
  const [bookingForm, setBookingForm] = useState<BookingFormData>({
    date: '', startTime: '', endTime: '', nome: '', cpf: '', whatsapp: '', bloco: '', unidade: ''
  });
  
  const [isAddingArea, setIsAddingArea] = useState(false);
  const [editingAreaId, setEditingAreaId] = useState<string | null>(null);
  const [areaForm, setAreaForm] = useState<Partial<CommonArea>>({
    name: '', capacity: 0, price: 0, description: '', rules: '', openTime: '08:00', closeTime: '22:00', imageUrl: ''
  });

  // INITIAL SYNC
  useEffect(() => {
    loadData();
  }, []);

  // AUTO-PREENCHIMENTO INTELIGENTE (PUXANDO DO BANCO/CONTEXTO)
  useEffect(() => {
    if (user && selectedArea) {
      setBookingForm(prev => ({
        ...prev,
        // Garantir que nenhum campo seja null ou undefined
        nome: user.nome || user.name || '',
        cpf: user.cpf || '',
        // Puxa telefone de várias fontes possíveis para garantir
        whatsapp: (user as any).whatsapp || (user as any).phone || (user as any).telefone || (user as any).celular || '',
        bloco: user.bloco || '',
        unidade: user.unidade || user.unit || ''
      }));
      setErrorMsg('');
      setSuccessMsg('');
    }
  }, [user, selectedArea]);

  const loadData = async () => {
    try {
      const [areasData, bookingsData] = await Promise.all([
        api.get('/facilities/areas'),
        api.get('/facilities/bookings')
      ]);
      setAreas(areasData.data || areasData);
      setBookings(bookingsData.data || bookingsData);

      try {
          const configRes = await api.get('/tenants/payment-config');
          if (configRes.data) setPaymentConfig(configRes.data);
      } catch (err: any) { 
          if (err.response?.status !== 500) {
             console.warn("Aviso: Configuração de pagamento não encontrada.", err); 
          }
      }
    } catch (error) {
      setErrorMsg("ERRO DE SINCRONIZAÇÃO: O banco de dados remoto não respondeu corretamente.");
    }
  };

  // --- HELPERS DE DADOS (CRUCIAL: TRATAMENTO DE OBJETOS ANINHADOS JAVA) ---
  
  const getAreaName = (booking: ExtendedBooking) => {
      // Prioridade: Objeto aninhado (JPA) -> ID (Fallback)
      if (booking.commonArea && booking.commonArea.name) return booking.commonArea.name;
      const id = booking.areaId || (booking.commonArea as any)?.id;
      return areas.find(a => a.id === id)?.name || "Área (Carregando...)";
  };

  const getEventDate = (booking: ExtendedBooking) => {
      const rawDate = booking.bookingDate || booking.date;
      if (!rawDate) return "N/D";
      try {
          // Ajuste para não converter timezone (apenas data visual)
          return rawDate.split('T')[0].split('-').reverse().join('/');
      } catch { return rawDate; }
  };

  const getCreatedAt = (booking: ExtendedBooking) => {
      const created = booking.createdAt || booking.created_at;
      if (!created) return "---";
      try {
          return new Date(created).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
      } catch { return "Inválido"; }
  };

  const getReceiptUrl = (booking: ExtendedBooking) => {
      return booking.receiptUrl || booking.receipt_url || booking.comprovante;
  };

  const isDateBlocked = (areaId: string, date: string) => {
    return bookings.some(b => {
        // Verifica objeto commonArea ou ID direto
        const bAreaId = b.commonArea?.id || b.areaId;
        const bDate = b.bookingDate || b.date;
        
        if (String(bAreaId) !== String(areaId) || bDate !== date) return false;
        
        const status = b.status as string; // Casting genérico para string
        
        // 1. Reservas aprovadas, confirmadas ou completas bloqueiam
        if (['APPROVED', 'CONFIRMED', 'COMPLETED'].includes(status)) return true;
        // 2. Se tem comprovante, bloqueia
        if (getReceiptUrl(b)) return true;
        // 3. Em análise bloqueia
        if (status === 'UNDER_ANALYSIS') return true;
        
        // 4. Pendentes bloqueiam apenas se dentro da janela de 30 min
        if (status === 'PENDING' && b.createdAt) {
            const created = new Date(b.createdAt).getTime();
            const now = new Date().getTime();
            // Se passou de 30 min, libera (não bloqueia). Se for <= 30, bloqueia.
            return (now - created) / 60000 <= 30;
        }
        return false;
    });
  };

  // --- FILTROS DE USUÁRIO (CORREÇÃO PARA OBJETO ANINHADO 'user') ---
  const myBookings = useMemo(() => {
      if (!user) return [];
      
      return bookings.filter(b => {
          // O Backend Java retorna user como objeto: { id: "...", nome: "..." }
          const backendUserId = b.user?.id || b.userId;
          // Comparação segura de strings
          return String(backendUserId) === String(user.id);
      }).sort((a, b) => new Date(b.createdAt || '').getTime() - new Date(a.createdAt || '').getTime());
  }, [bookings, user]);

  const groupedValidations = useMemo(() => {
      const pending = bookings.filter(b => {
          const status = b.status as string;
          return (status === 'UNDER_ANALYSIS' || status === 'PENDING') && !!getReceiptUrl(b);
      });

      const grouped: { [key: string]: ExtendedBooking[] } = {};
      pending.forEach(b => {
          const areaName = getAreaName(b);
          if (!grouped[areaName]) grouped[areaName] = [];
          grouped[areaName].push(b);
      });
      return grouped;
  }, [bookings, areas]);

  const archivedBookings = useMemo(() => {
      return bookings.filter(b => ['APPROVED', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'EXPIRED'].includes(b.status as string));
  }, [bookings]);

  // --- HANDLERS ---

  const handleReceiptUpload = async (e: React.ChangeEvent<HTMLInputElement>, bookingId: string) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingBookingId(bookingId);
    const formData = new FormData();
    formData.append('file', file);
    try {
        await api.post(`/facilities/bookings/${bookingId}/receipt`, formData);
        alert("PROTOCOLO DE SEGURANÇA: Comprovante enviado com sucesso. Aguarde validação.");
        await loadData(); 
    } catch (error: any) {
        alert("FALHA NO UPLOAD: O tempo pode ter expirado ou o arquivo é inválido.");
    } finally { setUploadingBookingId(null); }
  };

  const handleValidateReceipt = async (bookingId: string, isValid: boolean) => {
      if (!window.confirm(`SISTEMA DE AUDITORIA: Confirmar a ação de ${isValid ? 'APROVAR' : 'REPROVAR'}?`)) return;
      try {
          await api.patch(`/facilities/bookings/${bookingId}/validate`, { valid: isValid });
          await loadData();
          setSuccessMsg(isValid ? "Reserva Aprovada e Sincronizada." : "Reserva Rejeitada.");
      } catch (e) { 
          alert("ERRO DE COMUNICAÇÃO: Verifique suas permissões de Síndico."); 
      }
  };

  // Lógica de bloqueio estrito na seleção de data
  const handleDateSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
      const selectedDate = e.target.value;
      if (!selectedArea) return;

      if (isDateBlocked(selectedArea.id, selectedDate)) {
          alert("DATA INDISPONÍVEL: Este dia já está reservado por outro morador. Por favor, escolha outra data.");
          // Reseta o valor do input para vazio
          setBookingForm(prev => ({ ...prev, date: '' }));
      } else {
          setBookingForm(prev => ({ ...prev, date: selectedDate }));
      }
  };

  const handleCreateBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedArea || !user) return;
    
    const today = new Date().toISOString().split('T')[0];
    if (bookingForm.date < today) { setErrorMsg("OPERAÇÃO BLOQUEADA: Data passada."); return; }
    
    // Verificação dupla antes de enviar
    if (isDateBlocked(selectedArea.id, bookingForm.date)) { 
        setErrorMsg("CONFLITO DE AGENDA: Data indisponível."); 
        return; 
    }

    const price = selectedArea.price ?? 0;
    try {
      const response = await api.post('/facilities/bookings', {
        areaId: selectedArea.id,
        userId: user.id,
        unit: bookingForm.unidade,
        block: bookingForm.bloco,
        nome: bookingForm.nome,
        cpf: bookingForm.cpf,
        whatsapp: bookingForm.whatsapp,
        date: bookingForm.date,
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        billingType: price > 0 ? 'MANUAL' : 'FREE'
      });
      
      await loadData();

      if (price > 0) {
          setLastPaymentData(response.data);
          setIsPaymentModalOpen(true); 
      } else {
          setSuccessMsg("SUCESSO: Reserva confirmada no calendário.");
          setActiveTab('my-bookings');
          setSelectedArea(null);
      }
    } catch (e: any) { setErrorMsg("FALHA NO PROCESSO: Erro ao criar reserva."); }
  };

  const getStatusBadge = (booking: ExtendedBooking) => {
    // CORREÇÃO DO ERRO TS2367: Forçar o tipo para string para permitir comparação com 'EXPIRED'
    const status = booking.status as string; 
    const hasReceipt = !!getReceiptUrl(booking);

    if (status === 'EXPIRED') {
        return <span className="bg-red-500 text-white text-[10px] px-3 py-1 rounded-full font-black uppercase tracking-widest shadow-sm">Prazo Esgotado</span>;
    }

    if (status === 'PENDING') {
        if (booking.createdAt) {
            const created = new Date(booking.createdAt).getTime();
            // Verifica se passou 30 min E não tem recibo
            if ((new Date().getTime() - created) / 60000 > 30 && !hasReceipt) {
                return <span className="bg-red-500 text-white text-[10px] px-3 py-1 rounded-full font-black uppercase tracking-widest shadow-sm">Expirada</span>;
            }
        }
        return (
            <span className="bg-amber-500 text-white text-[10px] px-3 py-1 rounded-full font-black uppercase tracking-widest shadow-sm flex items-center gap-1.5">
                <Clock size={12}/> {hasReceipt ? 'Em Análise' : 'Aguardando Pix'}
            </span>
        );
    }

    switch (status) {
      case 'APPROVED': 
      case 'CONFIRMED': return <span className="bg-emerald-500 text-white text-[10px] px-3 py-1 rounded-full font-black uppercase tracking-widest shadow-sm flex items-center gap-1.5"><CheckCircle size={12}/> Aprovada</span>;
      case 'UNDER_ANALYSIS': return <span className="bg-indigo-600 text-white text-[10px] px-3 py-1 rounded-full font-black uppercase tracking-widest shadow-sm flex items-center gap-1.5"><Shield size={12}/> Em Análise</span>;
      case 'REJECTED': return <span className="bg-slate-400 text-white text-[10px] px-3 py-1 rounded-full font-black uppercase tracking-widest shadow-sm">Recusada</span>;
      default: return <span className="bg-slate-100 text-slate-500 text-[10px] px-3 py-1 rounded-full font-black uppercase tracking-widest border border-slate-200">{status}</span>;
    }
  };

  const handleSaveArea = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingAreaId) { await api.patch(`/facilities/areas/${editingAreaId}`, areaForm); } 
      else { await api.post('/facilities/areas', areaForm); }
      setIsAddingArea(false); setEditingAreaId(null);
      setAreaForm({ name: '', capacity: 0, price: 0, description: '', rules: '', openTime: '08:00', closeTime: '22:00', imageUrl: '' });
      loadData(); setSuccessMsg("Ativo atualizado.");
    } catch (e) { setErrorMsg("Erro ao salvar ativo."); }
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    try {
      const response = await api.post('/facilities/areas/upload', formData);
      setAreaForm(prev => ({ ...prev, imageUrl: response.data.url }));
      setSuccessMsg("Mídia sincronizada.");
    } catch (e) { setErrorMsg("Erro no upload."); } finally { setIsUploading(false); }
  };

  return (
    <div className="space-y-10 animate-in fade-in duration-700 pb-24 max-w-[1600px] mx-auto px-6">
      
      <PaymentModal 
          isOpen={isPaymentModalOpen}
          onClose={() => setIsPaymentModalOpen(false)}
          onGoToMyBookings={() => { 
            setIsPaymentModalOpen(false); 
            loadData(); 
            setActiveTab('my-bookings'); 
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }}
          data={lastPaymentData}
          config={paymentConfig}
      />

      {/* CABEÇALHO */}
      <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-8 bg-white p-10 rounded-[48px] border border-slate-200 shadow-2xl relative overflow-hidden group">
        <div className="relative z-10">
          <div className="flex items-center gap-4 mb-4">
              <div className="bg-slate-900 p-4 rounded-[24px] text-white shadow-xl rotate-3 group-hover:rotate-0 transition-transform">
                  <Building2 size={32}/>
              </div>
              <div>
                  <h1 className="text-5xl font-black text-slate-900 tracking-tighter uppercase leading-none">Votzz Spaces</h1>
                  <p className="text-slate-400 font-bold text-xs uppercase tracking-[3px] mt-2">Governança e Reservas</p>
              </div>
          </div>
        </div>
        
        <div className="flex bg-slate-100 p-2 rounded-[32px] border border-slate-200 shadow-inner relative z-10">
            <button onClick={() => setActiveTab('areas')} className={`px-10 py-4 rounded-[24px] text-xs font-black uppercase tracking-widest transition-all flex items-center gap-3 ${activeTab === 'areas' ? 'bg-white text-slate-900 shadow-xl' : 'text-slate-500 hover:text-slate-800'}`}>
                <CalendarDays size={16}/> Agendar
            </button>
            <button onClick={() => setActiveTab('my-bookings')} className={`px-10 py-4 rounded-[24px] text-xs font-black uppercase tracking-widest transition-all flex items-center gap-3 ${activeTab === 'my-bookings' ? 'bg-white text-slate-900 shadow-xl' : 'text-slate-500 hover:text-slate-800'}`}>
                <History size={16}/> Minha Agenda
            </button>
            {['MANAGER', 'SINDICO'].includes(user?.role || '') && (
              <button onClick={() => setActiveTab('manage')} className={`ml-2 px-10 py-4 rounded-[24px] text-xs font-black uppercase tracking-widest transition-all flex items-center gap-3 ${activeTab === 'manage' ? 'bg-slate-900 text-white shadow-2xl ring-4 ring-slate-100' : 'text-slate-500 hover:text-slate-800'}`}>
                 <Shield size={16}/> Gestão
                 {Object.keys(groupedValidations).length > 0 && (
                     <span className="bg-red-500 text-white w-2 h-2 rounded-full animate-ping"></span>
                 )}
              </button>
            )}
        </div>
      </div>

      <div className="grid gap-4 max-w-4xl mx-auto">
          {errorMsg && <div className="bg-red-600 text-white p-6 rounded-[32px] shadow-2xl border-4 border-red-500 flex items-center gap-4 font-black text-sm animate-in zoom-in-90"><AlertTriangle size={32}/> {errorMsg}</div>}
          {successMsg && <div className="bg-emerald-600 text-white p-6 rounded-[32px] shadow-2xl border-4 border-emerald-500 flex items-center gap-4 font-black text-sm animate-in zoom-in-90"><CheckCircle size={32}/> {successMsg}</div>}
      </div>

      {/* ABA: AGENDAMENTO */}
      {activeTab === 'areas' && (
        <div className="grid lg:grid-cols-4 gap-12">
          <div className="lg:col-span-3 grid md:grid-cols-2 xl:grid-cols-3 gap-8">
            {areas.map(area => (
              <div 
                key={area.id} 
                className={`group bg-white rounded-[48px] border-4 overflow-hidden transition-all cursor-pointer relative shadow-lg ${selectedArea?.id === area.id ? 'border-emerald-500 shadow-emerald-200' : 'border-transparent hover:border-slate-300'}`}
                onClick={() => { setSelectedArea(area); setErrorMsg(''); }}
              >
                <div className="h-72 bg-slate-100 relative overflow-hidden">
                  {area.imageUrl ? (
                      <img src={area.imageUrl} className="w-full h-full object-cover" />
                  ) : (
                      <div className="w-full h-full flex items-center justify-center text-slate-200 bg-slate-50">
                          <ImageIcon2 size={100} className="opacity-20"/>
                      </div>
                  )}
                  <div className="absolute top-6 right-6 bg-white/95 backdrop-blur px-5 py-2 rounded-2xl text-xs font-black text-slate-900 shadow-2xl border border-white uppercase tracking-[2px]">
                    {area.price > 0 ? `BRL ${area.price.toFixed(2)}` : 'Sem Custo'}
                  </div>
                </div>
                <div className="p-8">
                  <h3 className="font-black text-2xl text-slate-900 uppercase tracking-tighter leading-tight group-hover:text-emerald-600 transition-colors">{area.name}</h3>
                  <p className="text-slate-500 text-sm font-bold mt-2 uppercase opacity-80">{area.description || "Sem descrição."}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="lg:col-span-1">
              <div className="bg-white p-10 rounded-[56px] border border-slate-200 shadow-2xl sticky top-12 overflow-hidden">
                {selectedArea ? (
                  <form onSubmit={handleCreateBooking} className="space-y-6 relative z-10">
                    <div className="flex justify-between items-center border-b pb-6 border-slate-100">
                        <div>
                            <h3 className="font-black text-slate-900 text-lg uppercase tracking-widest">Protocolo Reserva</h3>
                            <p className="text-[10px] font-black text-emerald-500 uppercase tracking-[3px] mt-1">{selectedArea.name}</p>
                        </div>
                        <button type="button" onClick={() => setSelectedArea(null)} className="p-3 bg-red-50 text-red-500 rounded-2xl hover:bg-red-500 hover:text-white transition-all shadow-sm"><XCircle size={20}/></button>
                    </div>

                    <div className="space-y-4">
                        <div className="space-y-1">
                            <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">Responsável</label>
                            <input type="text" value={bookingForm.nome} onChange={e => setBookingForm({...bookingForm, nome: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" required />
                        </div>
                        
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">CPF</label>
                                <input type="text" value={bookingForm.cpf} onChange={e => setBookingForm({...bookingForm, cpf: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" required />
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">WhatsApp</label>
                                <input type="text" value={bookingForm.whatsapp} onChange={e => setBookingForm({...bookingForm, whatsapp: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" required />
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">Bloco</label>
                                <input type="text" value={bookingForm.bloco} onChange={e => setBookingForm({...bookingForm, bloco: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" required />
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">Unidade</label>
                                <input type="text" value={bookingForm.unidade} onChange={e => setBookingForm({...bookingForm, unidade: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" required />
                            </div>
                        </div>

                        <div className="pt-4 border-t border-slate-100 space-y-4">
                            <div className="space-y-1">
                                <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">Data do Evento</label>
                                {/* IMPLEMENTAÇÃO DE BLOQUEIO DE SELEÇÃO DE DATA */}
                                <input 
                                    type="date" 
                                    required 
                                    min={new Date().toISOString().split('T')[0]} 
                                    value={bookingForm.date} 
                                    onChange={handleDateSelect} // AQUI ESTÁ A LÓGICA DE BLOQUEIO
                                    className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" 
                                />
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-1">
                                    <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">Início</label>
                                    <input type="time" required value={bookingForm.startTime} onChange={e => setBookingForm({...bookingForm, startTime: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" />
                                </div>
                                <div className="space-y-1">
                                    <label className="text-[10px] font-black text-slate-400 uppercase ml-2 tracking-widest">Término</label>
                                    <input type="time" required value={bookingForm.endTime} onChange={e => setBookingForm({...bookingForm, endTime: e.target.value})} className="w-full p-4 bg-slate-50 border-2 border-slate-100 rounded-[20px] font-bold focus:outline-none focus:border-slate-900" />
                                </div>
                            </div>
                        </div>

                        <button type="submit" className="w-full bg-slate-900 text-white py-6 rounded-[28px] font-black text-xs uppercase tracking-[3px] shadow-2xl hover:bg-slate-800 transition-all active:scale-95 flex items-center justify-center gap-4 group mt-6">
                          EXECUTAR AGENDAMENTO <Zap size={18} className="text-emerald-400 group-hover:animate-bounce"/>
                        </button>
                    </div>
                  </form>
                ) : (
                  <div className="text-center py-24 opacity-20 group">
                      <div className="bg-slate-100 p-8 rounded-full inline-block mb-8 group-hover:scale-110 transition-transform">
                          <Activity size={80} className="text-slate-400" />
                      </div>
                      <p className="font-black text-slate-400 uppercase tracking-[5px] text-xs">Motor Votzz em Standby</p>
                      <p className="text-[9px] font-bold text-slate-400 uppercase mt-4">Selecione uma área ao lado</p>
                  </div>
                )}
              </div>
          </div>
        </div>
      )}

      {/* PAINEL DE GESTÃO */}
      {activeTab === 'manage' && (
        <div className="space-y-12 animate-in slide-in-from-bottom-6">
            
            <div className="flex gap-10 border-b-2 border-slate-100 overflow-x-auto pb-2">
                <button onClick={() => setManageSubTab('calendar')} className={`pb-6 text-[11px] font-black uppercase tracking-[4px] transition-all whitespace-nowrap flex items-center gap-3 ${manageSubTab === 'calendar' ? 'border-b-4 border-slate-900 text-slate-900' : 'text-slate-400 hover:text-slate-600'}`}>
                    <CalendarCheck size={18}/> 01. Mapa de Governança
                </button>
                <button onClick={() => setManageSubTab('config')} className={`pb-6 text-[11px] font-black uppercase tracking-[4px] transition-all whitespace-nowrap flex items-center gap-3 ${manageSubTab === 'config' ? 'border-b-4 border-slate-900 text-slate-900' : 'text-slate-400 hover:text-slate-600'}`}>
                    <Layers size={18}/> 02. Parâmetros & Ativos
                </button>
                <button onClick={() => setManageSubTab('validations')} className={`pb-6 text-[11px] font-black uppercase tracking-[4px] transition-all whitespace-nowrap flex items-center gap-3 ${manageSubTab === 'validations' ? 'border-b-4 border-slate-900 text-slate-900' : 'text-slate-400 hover:text-slate-600'}`}>
                    <Shield size={18}/> 03. Validação de Comprovantes
                    <span className="bg-red-500 text-white text-[10px] px-3 py-1 rounded-full shadow-lg animate-pulse">{Object.keys(groupedValidations).length}</span>
                </button>
            </div>

            {/* TAB CALENDÁRIO */}
            {manageSubTab === 'calendar' && (
                <div className="bg-white p-12 rounded-[56px] border border-slate-200 shadow-2xl relative overflow-hidden">
                    <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-12 relative z-10">
                        <div>
                            <h3 className="font-black text-3xl text-slate-900 uppercase tracking-tighter flex items-center gap-4">
                                <Activity className="text-slate-900"/> Tráfego de Reservas
                            </h3>
                        </div>
                        <div className="bg-slate-50 px-6 py-3 rounded-2xl border border-slate-100 shadow-sm flex items-center gap-3">
                            <Users className="text-indigo-600" size={18}/>
                            <span className="font-black text-slate-900 text-sm">{bookings.length} Registros</span>
                        </div>
                    </div>
                    <div className="overflow-x-auto relative z-10">
                        <table className="w-full">
                            <thead>
                                <tr className="text-left text-[10px] font-black text-slate-400 uppercase tracking-[3px] border-b-2 border-slate-50">
                                    <th className="pb-8">Sistema / Ativo</th>
                                    <th className="pb-8">Data do Evento</th>
                                    <th className="pb-8">Intervalo</th>
                                    <th className="pb-8">Solicitante</th>
                                    <th className="pb-8">Estado Atual</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {bookings.filter(b => !['CANCELLED', 'REJECTED', 'EXPIRED'].includes(b.status as string)).map(b => (
                                    <tr key={b.id} className="text-sm font-bold text-slate-700 hover:bg-slate-50 transition-all group">
                                        <td className="py-8 uppercase font-black text-slate-900 tracking-tighter text-base group-hover:text-indigo-600 transition-colors">
                                            {getAreaName(b)}
                                        </td>
                                        <td className="py-8">
                                            {getEventDate(b)}
                                        </td>
                                        <td className="py-8 text-indigo-600 font-black tracking-widest">{b.startTime} {' > '} {b.endTime}</td>
                                        <td className="py-8 flex items-center gap-3">
                                            <div className="bg-slate-100 p-2 rounded-lg text-slate-500">
                                                <UserIcon size={14}/>
                                            </div>
                                            <div>
                                                <p className="text-slate-900 font-black uppercase text-xs">{b.nome || "N/D"}</p>
                                                <p className="text-[10px] text-slate-400 uppercase tracking-widest">{b.unidade || b.unit}</p>
                                            </div>
                                        </td>
                                        <td className="py-8">{getStatusBadge(b)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* TAB CONFIG + GATEWAY (RESTAURADO) */}
            {manageSubTab === 'config' && (
                <div className="space-y-12">
                    <div className="bg-white p-12 rounded-[56px] border border-slate-200 shadow-2xl relative overflow-hidden">
                         <div className="flex justify-between items-center mb-12">
                            <h2 className="font-black text-3xl text-slate-900 uppercase tracking-tighter flex items-center gap-4">
                                <Database className="text-slate-900"/> Master Data Áreas
                            </h2>
                            <button onClick={() => setIsAddingArea(!isAddingArea)} className="bg-slate-900 text-white px-10 py-5 rounded-[24px] font-black text-xs hover:scale-105 transition-all shadow-2xl flex items-center gap-3 uppercase tracking-[3px]">
                                {isAddingArea ? <XCircle size={20}/> : <Plus size={20}/>}
                                {isAddingArea ? 'Interromper' : 'Novo Ativo'}
                            </button>
                        </div>

                        {isAddingArea && (
                            <form onSubmit={handleSaveArea} className="grid md:grid-cols-2 gap-10 mb-20 p-12 bg-slate-50 rounded-[48px] border-4 border-dashed border-slate-200 animate-in slide-in-from-top-4">
                                <div className="space-y-6">
                                    <div className="space-y-3">
                                        <label className="text-[11px] font-black text-slate-500 uppercase tracking-[4px] ml-1">Título do Recurso</label>
                                        <input type="text" placeholder="NOME DO ESPAÇO" value={areaForm.name} onChange={e => setAreaForm({...areaForm, name: e.target.value})} className="w-full p-5 bg-white border-2 border-slate-100 rounded-[28px] font-black text-base focus:border-slate-900 outline-none transition-colors" required />
                                    </div>
                                    <div className="grid grid-cols-2 gap-6">
                                        <div className="space-y-3">
                                            <label className="text-[11px] font-black text-slate-500 uppercase tracking-[4px] ml-1">Capacidade</label>
                                            <input type="number" placeholder="MAX PAX" value={areaForm.capacity || ''} onChange={e => setAreaForm({...areaForm, capacity: Number(e.target.value)})} className="w-full p-5 bg-white border-2 border-slate-100 rounded-[28px] font-black text-base" required />
                                        </div>
                                        <div className="space-y-3">
                                            <label className="text-[11px] font-black text-slate-500 uppercase tracking-[4px] ml-1">Tarifa (BRL)</label>
                                            <input type="number" step="0.01" placeholder="0.00" value={areaForm.price || ''} onChange={e => setAreaForm({...areaForm, price: Number(e.target.value)})} className="w-full p-5 bg-white border-2 border-slate-100 rounded-[28px] font-black text-base text-emerald-600" required />
                                        </div>
                                    </div>
                                    <div className="space-y-3">
                                        <label className="text-[11px] font-black text-slate-500 uppercase tracking-[4px] ml-1">Diretrizes de Uso</label>
                                        <textarea placeholder="DEFINA AS REGRAS..." rows={4} value={areaForm.description || ''} onChange={e => setAreaForm({...areaForm, description: e.target.value})} className="w-full p-5 bg-white border-2 border-slate-100 rounded-[28px] font-bold text-sm focus:border-slate-900 outline-none" />
                                    </div>
                                </div>
                                <div className="flex flex-col gap-6">
                                    <div className="border-4 border-dashed border-slate-200 rounded-[40px] flex-1 flex flex-col items-center justify-center bg-white relative overflow-hidden group">
                                        {areaForm.imageUrl ? (
                                            <>
                                                <img src={areaForm.imageUrl} className="w-full h-full object-cover" />
                                                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                                    <button type="button" onClick={() => setAreaForm({...areaForm, imageUrl: ''})} className="bg-red-600 text-white p-6 rounded-full shadow-2xl transition-transform hover:scale-110 active:scale-90"><Trash2 size={32}/></button>
                                                </div>
                                            </>
                                        ) : (
                                            <div className="text-center p-12 group-hover:scale-105 transition-transform">
                                                <div className="bg-slate-50 p-10 rounded-full inline-block mb-6 text-slate-300 shadow-inner">
                                                    <Server size={64}/>
                                                </div>
                                                <p className="text-sm font-black text-slate-400 uppercase tracking-[5px]">Upload Asset</p>
                                                <input type="file" onChange={handleImageUpload} className="absolute inset-0 opacity-0 cursor-pointer" />
                                            </div>
                                        )}
                                    </div>
                                    <button type="submit" disabled={isUploading} className="w-full bg-slate-900 text-white py-6 rounded-[32px] font-black text-xs uppercase tracking-[5px] shadow-2xl hover:bg-slate-800 disabled:opacity-50 transition-all flex items-center justify-center gap-4">
                                        <Save size={20}/> Persistir Ativo
                                    </button>
                                </div>
                            </form>
                        )}

                        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-10">
                            {areas.map(area => (
                                <div key={area.id} className="p-6 bg-white border-2 border-slate-50 rounded-[40px] shadow-xl hover:shadow-2xl transition-all flex flex-col gap-6 group hover:-translate-y-2">
                                    <div className="h-44 bg-slate-100 rounded-[32px] overflow-hidden relative shadow-inner">
                                        {area.imageUrl && <img src={area.imageUrl} className="w-full h-full object-cover group-hover:scale-125 transition-transform duration-1000" />}
                                        <div className="absolute bottom-4 left-4 bg-slate-900 text-white px-3 py-1 rounded-xl text-[9px] font-black border border-slate-700 shadow-xl uppercase tracking-widest">BRL {area.price.toFixed(2)}</div>
                                    </div>
                                    <div className="px-2">
                                        <p className="font-black text-slate-900 uppercase tracking-tighter truncate text-lg group-hover:text-indigo-600 transition-colors">{area.name}</p>
                                    </div>
                                    <div className="flex gap-3 p-1.5 bg-slate-50 rounded-[24px]">
                                        <button onClick={() => { setAreaForm(area); setEditingAreaId(area.id); setIsAddingArea(true); }} className="flex-1 py-4 text-indigo-600 bg-white rounded-2xl hover:bg-indigo-500 hover:text-white transition-all flex justify-center shadow-sm border border-slate-100"><Edit size={18}/></button>
                                        <button onClick={() => { if(window.confirm("Excluir?")) api.delete(`/facilities/areas/${area.id}`).then(loadData) }} className="flex-1 py-4 text-red-600 bg-white rounded-2xl hover:bg-red-600 hover:text-white transition-all flex justify-center shadow-sm border border-slate-100"><Trash2 size={18}/></button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="bg-slate-900 p-12 rounded-[56px] shadow-2xl relative overflow-hidden">
                        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-emerald-500 via-indigo-500 to-purple-500"></div>
                        <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-10 mb-12 relative z-10">
                            <div>
                                <h3 className="font-black text-3xl text-white flex items-center gap-4 uppercase tracking-tighter">
                                    <Banknote className="text-emerald-400" size={32}/> Gateway Financeiro Votzz
                                </h3>
                                <p className="text-slate-400 font-bold text-xs uppercase mt-3 tracking-widest leading-loose">
                                    Configuração de endpoint para recebimento via PIX Direto.<br/>
                                    <span className="text-red-400">Atenção:</span> Estes dados são visíveis para todos os moradores no checkout.
                                </p>
                            </div>
                            <button onClick={() => setIsEditingConfig(!isEditingConfig)} className={`px-10 py-5 rounded-[24px] text-[11px] font-black uppercase tracking-[3px] transition-all flex items-center gap-3 ${isEditingConfig ? 'bg-red-500 text-white shadow-xl animate-pulse' : 'bg-white/10 text-white hover:bg-white/20 border border-white/10 shadow-2xl'}`}>
                                {isEditingConfig ? <XCircle size={18}/> : <Settings size={18}/>}
                                {isEditingConfig ? 'Abortar Configuração' : 'Alterar Parâmetros'}
                            </button>
                        </div>
                        <form onSubmit={async (e) => { e.preventDefault(); try { await api.post('/tenants/payment-config', paymentConfig); alert("MASTER CONFIG: Parâmetros financeiros atualizados."); setIsEditingConfig(false); } catch (err) { alert("ERRO AO SALVAR."); } }} className="grid md:grid-cols-4 gap-8 relative z-10">
                            <div className="space-y-3">
                                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Instituição</label>
                                <input type="text" placeholder="NOME DO BANCO" value={paymentConfig.bankName || ''} onChange={e => setPaymentConfig({...paymentConfig, bankName: e.target.value})} disabled={!isEditingConfig} className="w-full p-5 bg-white/5 border border-white/10 rounded-[28px] font-black text-sm text-white focus:border-emerald-500 outline-none transition-all disabled:opacity-50 uppercase" />
                            </div>
                            <div className="space-y-3">
                                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Endpoint Pix (Chave)</label>
                                <input type="text" placeholder="CPF/CNPJ/EMAIL" value={paymentConfig.pixKey || ''} onChange={e => setPaymentConfig({...paymentConfig, pixKey: e.target.value})} disabled={!isEditingConfig} className="w-full p-5 bg-white/5 border border-white/10 rounded-[28px] font-black text-sm text-white focus:border-emerald-500 outline-none transition-all disabled:opacity-50" />
                            </div>
                            <div className="space-y-3">
                                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Agência</label>
                                <input type="text" placeholder="0000-0" value={paymentConfig.agency || ''} onChange={e => setPaymentConfig({...paymentConfig, agency: e.target.value})} disabled={!isEditingConfig} className="w-full p-5 bg-white/5 border border-white/10 rounded-[28px] font-black text-sm text-white focus:border-emerald-500 outline-none transition-all disabled:opacity-50" />
                            </div>
                            <div className="space-y-3">
                                <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest ml-1">Conta Master</label>
                                <input type="text" placeholder="000000000" value={paymentConfig.account || ''} onChange={e => setPaymentConfig({...paymentConfig, account: e.target.value})} disabled={!isEditingConfig} className="w-full p-5 bg-white/5 border border-white/10 rounded-[28px] font-black text-sm text-white focus:border-emerald-500 outline-none transition-all disabled:opacity-50" />
                            </div>
                            {isEditingConfig && (
                                <button className="md:col-span-4 bg-emerald-500 text-slate-900 py-6 rounded-[32px] font-black text-xs uppercase tracking-[5px] shadow-2xl shadow-emerald-500/20 hover:bg-emerald-400 transition-all mt-6 flex items-center justify-center gap-4">
                                    <ClipboardCheck size={20}/> Sincronizar Master Financial Config
                                </button>
                            )}
                        </form>
                    </div>
                </div>
            )}

            {/* TAB VALIDAÇÃO */}
            {manageSubTab === 'validations' && (
                <div className="space-y-10">
                    <div className="bg-white p-10 rounded-[40px] border-2 border-blue-600 shadow-2xl relative overflow-hidden">
                        <h3 className="font-black text-3xl text-slate-900 mb-12 flex items-center gap-4 uppercase tracking-tighter">
                            <ClipboardCheck className="text-blue-600" size={32}/> Validação de Comprovantes
                        </h3>
                        {Object.keys(groupedValidations).length === 0 ? (
                            <div className="text-center py-20 bg-slate-50 rounded-[32px] border border-dashed border-slate-200">
                                <p className="font-black text-slate-400 uppercase tracking-[4px] text-sm">Fila de validação limpa!</p>
                            </div>
                        ) : (
                            <div className="space-y-16">
                                {Object.entries(groupedValidations).map(([areaName, areaBookings]) => (
                                    <div key={areaName} className="space-y-6">
                                        <div className="flex items-center gap-6">
                                            <h4 className="bg-slate-900 text-white px-6 py-2 rounded-2xl text-xs font-black uppercase tracking-[2px] shadow-lg">{areaName}</h4>
                                            <div className="h-[2px] bg-slate-100 flex-1"></div>
                                        </div>
                                        <div className="grid gap-6">
                                            {areaBookings.map(b => (
                                                <div key={b.id} className="p-8 bg-white rounded-[32px] border border-slate-200 flex flex-col xl:flex-row justify-between items-start xl:items-center gap-8 hover:border-blue-400 transition-all relative group shadow-lg">
                                                    <div className="flex-1 space-y-4">
                                                        <div className="flex items-center gap-4">
                                                            <div className="bg-slate-900 p-3 rounded-2xl text-white"><UserCheck size={24}/></div>
                                                            <div>
                                                                <p className="font-black text-xl text-slate-900 uppercase tracking-tighter">{(b as any).nome || 'Morador'}</p>
                                                                <p className="text-[10px] font-black text-blue-600 uppercase tracking-widest flex items-center gap-1"><MapPin size={10}/> UNIDADE: {(b as any).unidade || (b as any).unit}</p>
                                                            </div>
                                                        </div>
                                                        
                                                        <div className="flex flex-wrap gap-4">
                                                            <div className="bg-slate-50 px-4 py-2 rounded-xl border border-slate-100 flex items-center gap-3">
                                                                <CalendarDays className="text-slate-400" size={16}/>
                                                                <span className="text-xs font-black text-slate-700 uppercase tracking-widest">{getEventDate(b)}</span>
                                                            </div>
                                                        </div>

                                                        <div className="pt-2">
                                                            {getReceiptUrl(b) && (
                                                                <a href={getReceiptUrl(b)} target="_blank" rel="noreferrer" className="inline-flex items-center gap-3 bg-blue-600 text-white px-6 py-3 rounded-2xl text-[10px] font-black hover:bg-blue-700 transition-all shadow-lg hover:scale-105">
                                                                    <FileText size={18}/> ABRIR ORIGINAL
                                                                </a>
                                                            )}
                                                        </div>
                                                    </div>

                                                    <div className="flex gap-4 w-full xl:w-auto">
                                                        <button onClick={() => handleValidateReceipt(b.id, true)} className="flex-1 xl:flex-none bg-emerald-600 text-white px-10 py-5 rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-emerald-700 shadow-xl flex items-center justify-center gap-2"><ThumbsUp size={18}/> APROVAR</button>
                                                        <button onClick={() => handleValidateReceipt(b.id, false)} className="flex-1 xl:flex-none bg-white text-red-600 px-10 py-5 rounded-2xl font-black text-xs uppercase tracking-widest border-2 border-red-100 hover:bg-red-50 flex items-center justify-center gap-2"><ThumbsDown size={18}/> REJEITAR</button>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
      )}

      {/* CONTEÚDO: MINHAS RESERVAS (ATUALIZADO COM DADOS DE PAGAMENTO E BLOQUEIO DE EXPIRADOS) */}
      {activeTab === 'my-bookings' && (
        <div className="bg-white rounded-[40px] border border-slate-200 overflow-hidden shadow-2xl animate-in slide-in-from-bottom-4">
            <div className="p-10 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                <div className="flex items-center gap-4">
                    <div className="bg-slate-900 p-3 rounded-2xl text-white">
                        <History size={24}/>
                    </div>
                    <div>
                        <h3 className="font-black text-slate-900 text-2xl tracking-tighter uppercase">Cronograma Pessoal</h3>
                        <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Gerencie seus agendamentos e anexos</p>
                    </div>
                </div>
            </div>
            <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead className="bg-white text-[10px] font-black text-slate-400 border-b border-slate-100 uppercase tracking-widest">
                    <tr>
                        <th className="px-10 py-6">Ativo Selecionado</th>
                        <th className="px-10 py-6">Data e Horário</th>
                        <th className="px-10 py-6 text-right">Ações Mandatórias / Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-50">
                    {/* USO DO FILTRO ROBUSTO AQUI */}
                    {myBookings.map(booking => {
                        // CÁLCULO DE EXPIRAÇÃO NO FRONTEND PARA GARANTIA DUPLA
                        const status = booking.status as string;
                        const created = new Date(booking.createdAt || booking.created_at || new Date().toISOString()).getTime();
                        const isTimeExpired = status === 'PENDING' && !booking.receiptUrl && (new Date().getTime() - created) > 30 * 60 * 1000;
                        const isTrulyExpired = status === 'EXPIRED' || isTimeExpired;

                        return (
                      <tr key={booking.id} className="hover:bg-slate-50/50 transition-colors group">
                        <td className="px-10 py-8">
                            <p className="font-black text-slate-900 text-xl uppercase tracking-tighter">{getAreaName(booking)}</p>
                            <div className="flex flex-col gap-1 mt-2">
                                <span className="text-[9px] font-black text-slate-300 uppercase tracking-widest">ID: {booking.id.slice(0,8)}</span>
                                <span className="text-[9px] font-bold text-slate-400">Solicitado em: {getCreatedAt(booking)}</span>
                            </div>
                            
                            {/* NOVO: CARD DE DADOS PIX NO CRONOGRAMA - SÓ APARECE SE PENDENTE E NÃO EXPIRADO */}
                            {booking.status === 'PENDING' && !isTrulyExpired && (
                                <div className="mt-4 p-4 bg-slate-100 rounded-2xl border border-slate-200 max-w-md animate-in slide-in-from-left-4">
                                    <div className="flex items-center gap-2 mb-2 text-[10px] font-black text-slate-500 uppercase tracking-widest border-b border-slate-200 pb-2">
                                        <Landmark size={14} className="text-slate-400"/> DADOS PARA PAGAMENTO (PIX)
                                    </div>
                                    <div className="grid grid-cols-3 gap-2 text-[10px] uppercase mb-3">
                                        <div><span className="block font-bold text-slate-400 mb-0.5">BANCO</span> <span className="font-black text-slate-800">{paymentConfig.bankName || "---"}</span></div>
                                        <div><span className="block font-bold text-slate-400 mb-0.5">AGÊNCIA</span> <span className="font-black text-slate-800">{paymentConfig.agency || "---"}</span></div>
                                        <div><span className="block font-bold text-slate-400 mb-0.5">CONTA</span> <span className="font-black text-slate-800">{paymentConfig.account || "---"}</span></div>
                                    </div>
                                    <div className="flex items-center gap-2 bg-white p-2 rounded-lg border border-slate-200 shadow-sm group/copy">
                                        <div className="bg-slate-100 p-1.5 rounded text-slate-400"><QrCode size={14}/></div>
                                        <code className="text-[10px] font-mono text-slate-600 flex-1 truncate font-bold">{paymentConfig.pixKey || "Chave não configurada"}</code>
                                        <button 
                                            onClick={() => {navigator.clipboard.writeText(paymentConfig.pixKey); alert("CHAVE COPIADA!");}} 
                                            className="text-indigo-600 hover:text-indigo-800 hover:bg-indigo-50 p-1.5 rounded transition-colors"
                                            title="Copiar Chave"
                                        >
                                            <Copy size={14}/>
                                        </button>
                                    </div>
                                </div>
                            )}
                        </td>
                        <td className="px-10 py-8">
                            <div className="flex items-center gap-4">
                                <div className="bg-white p-3 rounded-2xl border border-slate-200 text-center min-w-[70px] shadow-sm">
                                    <p className="text-[10px] font-black text-slate-400 uppercase">{new Date(booking.date || booking.bookingDate || '').toLocaleDateString('pt-BR', {month: 'short'})}</p>
                                    <p className="text-xl font-black text-slate-900 leading-none mt-1">{new Date(booking.date || booking.bookingDate || '').getDate() + 1}</p>
                                </div>
                                <div>
                                    <p className="text-sm font-black text-slate-700">{booking.startTime} ÀS {booking.endTime}</p>
                                    <div className="flex items-center gap-1 text-[9px] font-black text-indigo-500 uppercase tracking-widest mt-1">
                                        <Timer size={10}/> Slot Reservado
                                    </div>
                                </div>
                            </div>
                        </td>
                        <td className="px-10 py-8">
                            <div className="flex items-center justify-end gap-5">
                                <div className="transform group-hover:scale-105 transition-transform">
                                    {getStatusBadge(booking)}
                                </div>
                                
                                {booking.status === 'PENDING' && !getReceiptUrl(booking) && !isTrulyExpired && (
                                    <div className="flex items-center gap-3 bg-red-50 px-5 py-2.5 rounded-2xl border border-red-100 shadow-sm">
                                        <BookingTimer createdAt={booking.createdAt} onExpire={loadData} />
                                    </div>
                                )}

                                {/* LÓGICA DE UPLOAD REFORÇADA: EXPIRED NÃO ENTRA AQUI, SÓ PENDENTE OU EM ANÁLISE */}
                                {['PENDING', 'UNDER_ANALYSIS'].includes(status) && !isTrulyExpired ? (
                                    <div className="relative">
                                        <input 
                                            type="file" 
                                            id={`upload-${booking.id}`} 
                                            className="hidden" 
                                            onChange={(e) => handleReceiptUpload(e, booking.id)} 
                                            disabled={uploadingBookingId === booking.id} 
                                        />
                                        <label 
                                            htmlFor={`upload-${booking.id}`} 
                                            className={`cursor-pointer text-[10px] font-black px-7 py-4 rounded-2xl border-2 transition-all flex items-center gap-3 shadow-sm uppercase tracking-widest ${uploadingBookingId === booking.id ? 'bg-slate-100 text-slate-400 border-slate-200' : 'bg-white text-indigo-600 border-indigo-100 hover:border-indigo-600 active:scale-95'}`}
                                        >
                                            {uploadingBookingId === booking.id ? (
                                                <><Timer size={14} className="animate-spin"/> ENVIANDO...</>
                                            ) : (
                                                <><Upload size={16}/> {!getReceiptUrl(booking) ? 'ANEXAR COMPROVANTE' : 'SUBSTITUIR'}</>
                                            )}
                                        </label>
                                    </div>
                                ) : isTrulyExpired ? (
                                    <div className="flex items-center gap-2 text-red-500 font-black text-[10px] uppercase tracking-widest bg-red-50 px-4 py-2 rounded-xl border border-red-100 shadow-sm opacity-80 cursor-not-allowed">
                                        <Ban size={14}/> PRAZO ESGOTADO
                                    </div>
                                ) : null}
                            </div>
                        </td>
                      </tr>
                    );
                    })}
                    {myBookings.length === 0 && (
                        <tr>
                            <td colSpan={3} className="p-32 text-center opacity-30">
                                <CalendarDays size={80} className="mx-auto mb-6 text-slate-200" />
                                <p className="font-black uppercase tracking-[8px] text-xs">Sem Atividades Registradas</p>
                            </td>
                        </tr>
                    )}
                  </tbody>
                </table>
            </div>
        </div>
      )}
      
      <div className="flex flex-col md:flex-row justify-between items-center gap-8 pt-12 border-t border-slate-100 opacity-40">
          <div className="flex items-center gap-4">
              <div className="bg-slate-900 p-2 rounded-lg text-white"><Cpu size={20}/></div>
              <div>
                  <p className="text-[10px] font-black uppercase tracking-[4px] leading-none text-slate-900">Votzz Core Engine © 2026</p>
                  <p className="text-[9px] font-bold uppercase tracking-[2px] mt-1">Desenvolvido para Governança Autônoma</p>
              </div>
          </div>
          <div className="flex items-center gap-10">
              <span className="text-[9px] font-black uppercase tracking-[2px] flex items-center gap-2"><Globe size={12}/> Latência: 8ms</span>
              <span className="text-[9px] font-black uppercase tracking-[2px] flex items-center gap-2"><HardDrive size={12}/> DB Sync: 100%</span>
              <span className="text-[9px] font-black uppercase tracking-[2px] flex items-center gap-2 text-emerald-600"><RefreshCw size={12} className="animate-spin"/> Online</span>
          </div>
      </div>
    </div>
  );
};

export default Spaces;
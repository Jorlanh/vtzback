import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Check, Calculator, Building, Crown, Smartphone, 
  AlertCircle, ArrowLeft, Loader2, User, Eye, EyeOff, ArrowRight, Banknote, Copy, Shield, Lock, X, FileText, Tag, UserCheck, Sparkles, TrendingDown
} from 'lucide-react';
import api from '../services/api'; 
import { Logo } from '../components/Logo'; 

// SEGURANÇA: IDs agora vêm das Variáveis de Ambiente (.env)
const PLAN_IDS = {
  ESSENCIAL: import.meta.env.VITE_PLAN_ESSENTIAL || '',
  BUSINESS: import.meta.env.VITE_PLAN_BUSINESS || '',
  CUSTOM: import.meta.env.VITE_PLAN_CUSTOM || ''
};

const COMMON_BENEFITS = [
  "Aplicativo Votzz (iOS/Android)", 
  "Votação em Tempo Real",
  "Assembleia Digital ao Vivo",
  "Ata Automática",
  "Auditoria Criptografada",
  "Lista de Presença Digital",
  "Convocações Digitais",
  "Votos Ilimitados",
  "Acesso Moradores e Conselho",
  "Dashboard Completo"
];

// Validador de CPF
const isValidCPF = (cpf: string) => {
  const cleanCPF = cpf.replace(/[^\d]/g, '');
  if (cleanCPF.length !== 11 || /^(\d)\1+$/.test(cleanCPF)) return false;
  let soma = 0, resto;
  for (let i = 1; i <= 9; i++) soma += parseInt(cleanCPF.substring(i - 1, i)) * (11 - i);
  resto = (soma * 10) % 11;
  if ((resto === 10) || (resto === 11)) resto = 0;
  if (resto !== parseInt(cleanCPF.substring(9, 10))) return false;
  soma = 0;
  for (let i = 1; i <= 10; i++) soma += parseInt(cleanCPF.substring(i - 1, i)) * (12 - i);
  resto = (soma * 10) % 11;
  if ((resto === 10) || (resto === 11)) resto = 0;
  if (resto !== parseInt(cleanCPF.substring(10, 11))) return false;
  return true;
};

// Modal de Termos
const TermsModal = ({ isOpen, onClose, type }: { isOpen: boolean; onClose: () => void; type: 'terms' | 'privacy' }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-2xl max-h-[80vh] rounded-2xl shadow-2xl flex flex-col relative">
        <div className="flex items-center justify-between p-4 border-b border-slate-100">
          <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            {type === 'terms' ? <FileText className="text-emerald-600"/> : <Lock className="text-emerald-600"/>}
            {type === 'terms' ? 'Termos de Uso' : 'Política de Privacidade'}
          </h3>
          <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-full transition-colors">
            <X size={20} className="text-slate-500"/>
          </button>
        </div>
        
        <div className="p-6 overflow-y-auto custom-scrollbar text-sm text-slate-600 space-y-4 leading-relaxed text-justify">
          {type === 'terms' ? (
            <>
              <p><strong>1. ACEITAÇÃO.</strong> Ao utilizar o Votzz, você concorda com estes termos. O serviço é fornecido "como está".</p>
              <p><strong>2. USO DO SERVIÇO.</strong> O Votzz oferece ferramentas para gestão de condomínios. O usuário é responsável pela veracidade dos dados inseridos, incluindo número de unidades e blocos.</p>
              <p><strong>3. PAGAMENTOS.</strong> Os pagamentos são processados por terceiros (Kiwify/Asaas). A liberação do acesso ocorre após a confirmação.</p>
              <p><strong>4. LIMITES DO PLANO.</strong> O usuário deve respeitar os limites de unidades do plano contratado: Essencial (até 30), Business (até 80) e Custom (acima de 80).</p>
              <p><strong>5. CANCELAMENTO.</strong> O cancelamento pode ser solicitado conforme as regras do Código de Defesa do Consumidor.</p>
            </>
          ) : (
            <>
              <p><strong>1. COLETA DE DADOS.</strong> Coletamos nome, e-mail, CPF e dados do condomínio para prestação do serviço e emissão de notas fiscais.</p>
              <p><strong>2. USO DOS DADOS.</strong> Seus dados não são vendidos. Eles são usados para autenticação, segurança e comunicações essenciais.</p>
              <p><strong>3. SEGURANÇA.</strong> Utilizamos criptografia e práticas de segurança de mercado para proteger suas informações.</p>
              <p><strong>4. COMPARTILHAMENTO.</strong> Compartilhamos dados apenas com gateways de pagamento para processar sua assinatura.</p>
            </>
          )}
        </div>

        <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end">
          <button onClick={onClose} className="bg-emerald-600 text-white px-6 py-2 rounded-lg font-bold hover:bg-emerald-700 transition-colors">
            Entendi e Concordo
          </button>
        </div>
      </div>
    </div>
  );
};

export default function Pricing() {
  const navigate = useNavigate();

  // State para o Simulador (Tela Principal)
  const [units, setUnits] = useState<number>(50);
  const [cycle, setCycle] = useState<'TRIMESTRAL' | 'ANUAL'>('ANUAL');
  const [selectedPlanName, setSelectedPlanName] = useState('Business');

  const [showRegister, setShowRegister] = useState(false);
  const [loadingCnpj, setLoadingCnpj] = useState(false);
  const [loadingSubmit, setLoadingSubmit] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const [pixData, setPixData] = useState<{payload: string, image: string} | null>(null);
  const [copied, setCopied] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalType, setModalType] = useState<'terms' | 'privacy'>('terms');
  const [termsAccepted, setTermsAccepted] = useState(false);

  // State do Formulário (Dentro do Modal)
  const [registerData, setRegisterData] = useState({
    planId: '',
    isTrial: false,
    cycle: 'ANUAL',
    qtyUnits: 0,
    qtyBlocks: 1,
    condoName: '',
    cnpj: '',
    cep: '',
    logradouro: '',
    numero: '',
    bairro: '',
    cidade: '',
    estado: '',
    pontoReferencia: '',
    secretKeyword: '',
    nameSyndic: '',
    emailSyndic: '',
    cpfSyndic: '',
    whatsappSyndic: '',
    passwordSyndic: '',
    confirmPassword: '',
    couponCode: '',
    affiliateCode: ''
  });

  // Atualiza o card selecionado no simulador
  useEffect(() => {
    if (units <= 30) setSelectedPlanName('Essencial');
    else if (units <= 80) setSelectedPlanName('Business');
    else setSelectedPlanName('Custom');
  }, [units]);

  // Lógica central de cálculo de preço
  const getPlanDetails = (plan: string, overrideUnits?: number) => {
    const unitsToCalc = overrideUnits !== undefined ? overrideUnits : units;

    let monthlyBase = 0;
    let id = '';
    let min = 1, max = 30;

    if (plan === 'Essencial') { 
      monthlyBase = 190.00; 
      id = PLAN_IDS.ESSENCIAL; 
      max = 30; 
    }
    else if (plan === 'Business') { 
      monthlyBase = 349.00; 
      id = PLAN_IDS.BUSINESS; 
      min = 31; max = 80; 
    }
    else { 
      // Lógica Custom
      const safeUnits = Math.max(unitsToCalc, 81);
      monthlyBase = 349.00 + (safeUnits - 80) * 1.50; 
      id = PLAN_IDS.CUSTOM; 
      min = 81; max = 99999; 
    }
    
    const fullPrice = cycle === 'TRIMESTRAL' ? monthlyBase * 3 : (monthlyBase * 12);
    const finalPrice = cycle === 'ANUAL' ? fullPrice * 0.8 : fullPrice;
    
    // Calcula o valor mensal proporcional para exibição
    const monthlyEquivalent = finalPrice / (cycle === 'TRIMESTRAL' ? 3 : 12);

    return { id, finalPrice, fullPrice, min, max, monthlyEquivalent };
  };

  const handleSelectPlan = (planName: string, isTrial: boolean) => {
    const details = getPlanDetails(planName);
    
    let initialUnits = units;
    if (initialUnits < details.min) initialUnits = details.min;
    if (initialUnits > details.max) initialUnits = details.max;

    setRegisterData(prev => ({
      ...prev,
      planId: details.id,
      isTrial,
      cycle,
      qtyUnits: initialUnits, 
      qtyBlocks: 1
    }));
    
    setSelectedPlanName(planName);
    setPixData(null);
    setTermsAccepted(false);
    setShowRegister(true);
    window.scrollTo(0, 0);
  };

  const handleUnitChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let val = parseInt(e.target.value) || 0;
    setRegisterData({ ...registerData, qtyUnits: val });
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setRegisterData({ ...registerData, [e.target.name]: e.target.value });
    if (e.target.name === 'cep') {
      const cleanCep = e.target.value.replace(/\D/g, '');
      if (cleanCep.length === 8) fetchCep(cleanCep);
    }
  };

  const openModal = (type: 'terms' | 'privacy') => {
    setModalType(type);
    setModalOpen(true);
  };

  const fetchCep = async (cep: string) => {
    try {
      const res = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
      const data = await res.json();
      if (!data.erro) {
        setRegisterData(prev => ({
          ...prev,
          logradouro: data.logradouro,
          bairro: data.bairro,
          cidade: data.localidade,
          estado: data.uf
        }));
        document.getElementById('numeroInput')?.focus();
      }
    } catch (e) { console.error("Erro CEP", e); }
  };

  const handleCnpjBlur = async (e: React.FocusEvent<HTMLInputElement>) => {
    const rawCnpj = e.target.value.replace(/\D/g, '');

    if (rawCnpj.length === 14) {
      setLoadingCnpj(true);
      setErrorMsg(''); 
      
      try {
        let response = await fetch(`https://opencnpj.org/v1/${rawCnpj}`);
        if (!response.ok) throw new Error('OpenCNPJ falhou');
        
        const data = await response.json();
        setRegisterData(prev => ({
          ...prev,
          condoName: data.nome || data.razao_social || prev.condoName,
          cep: data.cep ? data.cep.replace(/\D/g, '') : prev.cep,
          logradouro: data.logradouro || prev.logradouro,
          numero: data.numero || prev.numero,
          bairro: data.bairro || prev.bairro,
          cidade: data.municipio || prev.cidade,
          estado: data.uf || prev.estado
        }));

      } catch (err) {
        console.warn("OpenCNPJ falhou, tentando BrasilAPI...");
        try {
          const resBR = await fetch(`https://brasilapi.com.br/api/cnpj/v1/${rawCnpj}`);
          if (resBR.ok) {
            const dataBR = await resBR.json();
            setRegisterData(prev => ({
              ...prev,
              condoName: dataBR.razao_social || dataBR.nome_fantasia || prev.condoName,
              cep: dataBR.cep ? dataBR.cep.replace(/\D/g, '') : prev.cep,
              logradouro: dataBR.logradouro || prev.logradouro,
              numero: dataBR.numero || prev.numero,
              bairro: dataBR.bairro || prev.bairro,
              cidade: dataBR.municipio || prev.cidade,
              estado: dataBR.uf || prev.estado
            }));
          }
        } catch (err2) { console.error("Erro fatal nas APIs de CNPJ", err2); }
      } finally {
        setLoadingCnpj(false);
      }
    }
  };

  const handleCopyPix = () => {
    if (pixData?.payload) {
      navigator.clipboard.writeText(pixData.payload);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!termsAccepted) {
      setErrorMsg("Você deve ler e aceitar os Termos de Uso e Privacidade.");
      return;
    }

    if (registerData.passwordSyndic !== registerData.confirmPassword) {
      setErrorMsg("As senhas não conferem!");
      return;
    }
    
    if (!isValidCPF(registerData.cpfSyndic)) {
      setErrorMsg("CPF inválido.");
      return;
    }

    if (!registerData.secretKeyword || registerData.secretKeyword.length < 4) {
      setErrorMsg("Defina uma palavra-chave com pelo menos 4 caracteres.");
      return;
    }

    const inputUnits = Number(registerData.qtyUnits);
    const details = getPlanDetails(selectedPlanName);

    if (inputUnits < details.min || inputUnits > details.max) {
      setErrorMsg(`Para o plano ${selectedPlanName}, o número de unidades deve ser entre ${details.min} e ${details.max}.`);
      return;
    }

    setLoadingSubmit(true);
    setErrorMsg('');

    try {
      const payload = {
        ...registerData,
        cnpj: registerData.cnpj.replace(/\D/g, ''),
        cpfSyndic: registerData.cpfSyndic.replace(/\D/g, ''), 
        whatsappSyndic: registerData.whatsappSyndic.replace(/\D/g, ''),
        cep: registerData.cep.replace(/\D/g, ''),
        qtyUnits: Number(registerData.qtyUnits),
        qtyBlocks: Number(registerData.qtyBlocks || 1),
        isTrial: Boolean(registerData.isTrial),
        planId: details.id,
        pontoReferencia: registerData.pontoReferencia || "",
        couponCode: registerData.couponCode || "",
        affiliateCode: registerData.affiliateCode || ""
      };

      const res = await api.post('/auth/register-condo', payload);
      const { redirectUrl, pixPayload, pixImage } = res.data;

      if (registerData.isTrial) {
        navigate('/login', { state: { message: 'Cadastro realizado! Faça login.' } });
      } else {
        if (pixPayload && pixImage) {
          setPixData({ payload: pixPayload, image: pixImage });
        } else if (redirectUrl) {
          window.location.href = redirectUrl;
        } else {
          navigate('/login', { state: { message: 'Solicitação enviada. Verifique seu e-mail.' } });
        }
      }
    } catch (err: any) {
      console.error("ERRO COMPLETO:", err.response?.data);
      const msg = err.response?.data?.message || err.response?.data?.error || "Erro ao realizar cadastro.";
      if (typeof msg === 'string' && (msg.includes("duplicate") || msg.includes("viola a restrição"))) {
        setErrorMsg("Este CNPJ ou E-mail já está cadastrado.");
      } else {
        setErrorMsg(typeof msg === 'string' ? msg : "Erro de validação: Verifique os dados.");
      }
    } finally {
      setLoadingSubmit(false);
    }
  };

  const getCardClasses = (planName: string) => {
    const isActive = selectedPlanName === planName;
    const baseClasses = "pricing-card border rounded-2xl p-8 transition-all flex flex-col justify-between min-h-[800px]";
    if (isActive) {
      return `${baseClasses} bg-[#022c22] text-white border-emerald-500 ring-2 ring-emerald-500/50 shadow-2xl z-10`;
    }
    return `${baseClasses} bg-white text-slate-800 border-slate-200 opacity-90 hover:opacity-100 z-0`;
  };

  const BenefitsList = ({ isDark }: { isDark: boolean }) => (
    <ul className={`space-y-3 text-sm mb-8 flex-1 text-left ${isDark ? 'text-emerald-100' : 'text-slate-600'}`}>
      <li className="flex gap-2 items-start font-bold">
        <Smartphone className="text-emerald-400 flex-shrink-0" size={18}/> 
        <span>Aplicativo Votzz (iOS/Android)</span>
      </li>
      {COMMON_BENEFITS.slice(1).map((benefit, index) => (
        <li key={index} className="flex gap-2 items-start">
          <Check className="text-emerald-400 flex-shrink-0" size={18}/> 
          <span>{benefit}</span>
        </li>
      ))}
    </ul>
  );

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      
      <TermsModal isOpen={modalOpen} onClose={() => setModalOpen(false)} type={modalType} />

      {/* BANNER 30 DIAS GRÁTIS */}
      <div className="bg-emerald-600 text-white py-3 px-4 text-center text-sm font-bold flex items-center justify-center gap-2 animate-pulse">
        <Sparkles size={16} /> OBSERVAÇÃO: Teste qualquer plano por 30 dias totalmente grátis! <Sparkles size={16} />
      </div>

      {showRegister && pixData && (
        <div className="fixed inset-0 z-50 bg-slate-50 flex items-center justify-center p-4 font-sans text-slate-800 animate-in fade-in duration-300">
          <div className="bg-white w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden p-8 text-center">
            <div className="mb-6 flex justify-center"><Logo theme="light" /></div>
            <div className="bg-emerald-50 text-emerald-800 p-4 rounded-xl border border-emerald-100 mb-6">
              <Banknote className="mx-auto mb-2 w-8 h-8"/>
              <h2 className="text-xl font-bold">Pagamento via Pix</h2>
              <p className="text-sm">Escaneie o QR Code para ativar sua conta Custom.</p>
            </div>
            
            <div className="bg-slate-100 p-4 rounded-xl border border-slate-200 inline-block mb-6 shadow-inner">
              <img src={`data:image/png;base64,${pixData.image}`} alt="QR Code Pix" className="w-48 h-48 mx-auto mix-blend-multiply" />
            </div>

            <div className="max-w-xs mx-auto mb-6 text-left">
              <label className="block text-xs font-bold text-slate-400 uppercase mb-2">Pix Copia e Cola</label>
              <div className="flex gap-2">
                <input readOnly value={pixData.payload} className="flex-1 p-2 border rounded-lg text-xs font-mono bg-slate-50 text-slate-600 outline-none" />
                <button type="button" onClick={handleCopyPix} className="bg-emerald-600 text-white p-2 rounded-lg hover:bg-emerald-700 transition-colors">
                  {copied ? <Check size={18}/> : <Copy size={18}/>}
                </button>
              </div>
              {copied && <p className="text-xs text-emerald-600 mt-1 font-bold text-center">Copiado!</p>}
            </div>
            
            <button onClick={() => navigate('/login')} className="w-full bg-[#022c22] text-white font-bold py-3 rounded-xl hover:bg-emerald-800 transition-all">
              Já realizei o pagamento (Ir para Login)
            </button>
          </div>
        </div>
      )}

      {showRegister && !pixData && (
        <div className="fixed inset-0 z-40 bg-slate-50 overflow-y-auto">
          <div className="min-h-screen flex items-center justify-center p-4">
            <div className="bg-white w-full max-w-5xl rounded-2xl shadow-2xl overflow-hidden flex flex-col md:flex-row animate-in fade-in zoom-in duration-300 relative">
              
              <button onClick={() => setShowRegister(false)} className="md:hidden absolute top-4 right-4 z-50 bg-white/20 p-2 rounded-full text-white"><X size={20}/></button>

              <div className="bg-[#022c22] p-8 text-white md:w-1/3 flex flex-col justify-between relative overflow-hidden">
                <div className="relative z-10">
                  <div className="mb-8 scale-90 origin-left"><Logo theme="light" /></div>

                  <div className="mb-6 bg-emerald-500/20 w-16 h-16 rounded-2xl flex items-center justify-center border border-emerald-500/30">
                    <Building className="text-emerald-400" size={32} />
                  </div>

                  <h2 className="text-2xl font-bold mb-4">Finalizar Cadastro</h2>
                  <p className="text-emerald-100 text-sm mb-6">Preencha os dados do condomínio para liberar o sistema.</p>
                  
                  <div className="bg-emerald-900/50 p-4 rounded-xl border border-emerald-800 shadow-lg">
                    <p className="text-[10px] text-emerald-300 font-bold uppercase tracking-widest mb-1">Plano Selecionado</p>
                    <p className="text-xl font-bold">{selectedPlanName}</p>
                    <div className="h-px bg-emerald-800 my-2"></div>
                    <div className="flex justify-between text-sm text-emerald-200">
                      <span>{registerData.qtyUnits} Unidades</span>
                      <span>{cycle === 'TRIMESTRAL' ? 'Trimestral' : 'Anual'}</span>
                    </div>
                    
                    <div className="mt-3 text-2xl font-black text-white">
                      {Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
                        getPlanDetails(selectedPlanName, registerData.qtyUnits).finalPrice
                      )}
                    </div>
                    
                    {registerData.isTrial && (
                      <div className="mt-2 bg-emerald-500 text-slate-900 text-[10px] px-2 py-1 rounded font-bold inline-block uppercase tracking-wide">
                        Teste Grátis Ativado
                      </div>
                    )}
                  </div>
                </div>
                
                <button onClick={() => setShowRegister(false)} className="mt-8 flex items-center gap-2 text-emerald-300 hover:text-white transition-colors text-sm relative z-10">
                  <ArrowLeft size={16}/> Voltar e Alterar Plano
                </button>
              </div>
              
              <div className="p-8 md:w-2/3 bg-white overflow-y-auto max-h-[90vh] custom-scrollbar">
                <form onSubmit={handleRegisterSubmit} className="space-y-5">
                  {errorMsg && (
                    <div className="bg-red-50 text-red-600 p-4 rounded-xl flex items-center gap-2 text-sm border border-red-100 animate-pulse">
                      <AlertCircle size={20}/> {errorMsg}
                    </div>
                  )}
                  
                  {/* Dados do Condomínio */}
                  <div>
                    <h3 className="font-bold text-slate-800 flex items-center gap-2 border-b border-slate-100 pb-2 mb-4 text-lg">
                      <Building className="text-emerald-600" size={20}/> Dados do Condomínio
                    </h3>
                    
                    <div className="grid md:grid-cols-2 gap-4">
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">CNPJ (Busca Auto)</label>
                        <div className="relative">
                          <input name="cnpj" value={registerData.cnpj} onChange={handleInputChange} onBlur={handleCnpjBlur} className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500 transition-all font-mono" placeholder="00.000.000/0000-00" maxLength={18} required />
                          {loadingCnpj && <Loader2 className="absolute right-3 top-3 animate-spin text-emerald-500" size={20}/>}
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Razão Social</label>
                        <input name="condoName" value={registerData.condoName} onChange={handleInputChange} className="w-full p-3 border border-slate-200 rounded-xl bg-slate-50 focus:bg-white outline-none focus:ring-2 focus:ring-emerald-500 transition-all" placeholder="Nome do Condomínio" required />
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4 mt-3">
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">CEP</label>
                        <input name="cep" value={registerData.cep} onChange={handleInputChange} placeholder="00000-000" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required/>
                      </div>
                      <div className="col-span-2 space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Endereço</label>
                        <input name="logradouro" value={registerData.logradouro} onChange={handleInputChange} placeholder="Rua / Avenida" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required/>
                      </div>
                    </div>
                    <div className="grid grid-cols-3 gap-4 mt-3">
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Número</label>
                        <input id="numeroInput" name="numero" value={registerData.numero} onChange={handleInputChange} placeholder="Nº" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required/>
                      </div>
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Bairro</label>
                        <input name="bairro" value={registerData.bairro} onChange={handleInputChange} placeholder="Bairro" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required/>
                      </div>
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Cidade</label>
                        <input name="cidade" value={registerData.cidade} onChange={handleInputChange} placeholder="Cidade" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required/>
                      </div>
                    </div>

                    <div className="mt-4 bg-emerald-50 p-3 rounded-xl border border-emerald-100">
                      <label className="text-[10px] font-black text-emerald-600 uppercase ml-1 flex items-center gap-1">
                        <Shield size={12}/> Palavra-Chave de Acesso (Moradores)
                      </label>
                      <input name="secretKeyword" value={registerData.secretKeyword} onChange={handleInputChange} className="w-full p-3 border border-emerald-200 rounded-xl bg-white outline-none focus:ring-2 focus:ring-emerald-500 transition-all font-bold text-emerald-900 mt-1" placeholder="Ex: VIVERBEM2026" required />
                      <p className="text-[10px] text-slate-500 mt-1 ml-1">Esta palavra será usada pelos moradores para se cadastrarem.</p>
                    </div>
                  </div>
                  
                  {/* Nº Unidades e Blocos */}
                  <div className="grid grid-cols-2 gap-4 mt-3 pt-3 border-t border-slate-100">
                    <div className="space-y-1">
                      <label className="text-[10px] font-black text-emerald-600 uppercase ml-1">Nº Unidades (Total)</label>
                      <input 
                        type="number"
                        name="qtyUnits" 
                        value={registerData.qtyUnits} 
                        onChange={handleUnitChange} 
                        className="w-full p-3 border border-emerald-200 rounded-xl bg-emerald-50/30 outline-none focus:ring-2 focus:ring-emerald-500 font-bold text-center" 
                        min="1"
                        required 
                      />
                      <p className="text-[10px] text-slate-400 ml-1">
                        {selectedPlanName === 'Essencial' ? 'Máx: 30' : selectedPlanName === 'Business' ? '31 a 80' : 'Acima de 80'}
                      </p>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Qtd. Blocos/Torres</label>
                      <input 
                        type="number"
                        name="qtyBlocks" 
                        value={registerData.qtyBlocks} 
                        onChange={handleInputChange} 
                        className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500 text-center" 
                        min="1"
                        placeholder="Ex: 1"
                        required 
                      />
                    </div>
                  </div>

                  {/* Cupom + Afiliado */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3">
                    {selectedPlanName === 'Custom' && (
                      <div className="bg-emerald-50 p-3 rounded-lg border border-emerald-100">
                        <label className="text-[10px] font-black text-emerald-700 uppercase flex items-center gap-1">
                          <Tag size={12}/> Cupom de Desconto
                        </label>
                        <input 
                          name="couponCode" 
                          value={registerData.couponCode} 
                          onChange={handleInputChange} 
                          className="w-full p-2 mt-1 border border-emerald-200 rounded bg-white text-sm focus:border-emerald-500 outline-none" 
                          placeholder="Código promocional" 
                        />
                      </div>
                    )}

                    <div className={`bg-blue-50 p-3 rounded-lg border border-blue-100 ${selectedPlanName !== 'Custom' ? 'md:col-span-2' : ''}`}>
                      <label className="text-[10px] font-black text-blue-700 uppercase flex items-center gap-1">
                        <UserCheck size={12}/> Código do Vendedor
                      </label>
                      <input 
                        name="affiliateCode" 
                        value={registerData.affiliateCode} 
                        onChange={handleInputChange} 
                        className="w-full p-2 mt-1 border border-blue-200 rounded bg-white text-sm focus:border-blue-500 outline-none" 
                        placeholder="Código do parceiro" 
                      />
                    </div>
                  </div>

                  {/* Dados do Síndico */}
                  <div>
                    <h3 className="font-bold text-slate-800 flex items-center gap-2 border-b border-slate-100 pb-2 mb-4 text-lg mt-6">
                      <User className="text-emerald-600" size={20}/> Dados do Síndico
                    </h3>
                    <div className="space-y-3">
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Nome Completo</label>
                        <input name="nameSyndic" value={registerData.nameSyndic} onChange={handleInputChange} placeholder="Nome do Síndico" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required/>
                      </div>
                      <div className="grid md:grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <label className="text-[10px] font-black text-slate-400 uppercase ml-1">E-mail (Login)</label>
                          <input name="emailSyndic" value={registerData.emailSyndic} onChange={handleInputChange} placeholder="seu@email.com" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" type="email" required/>
                        </div>
                        <div className="space-y-1">
                          <label className="text-[10px] font-black text-slate-400 uppercase ml-1">WhatsApp</label>
                          <input name="whatsappSyndic" value={registerData.whatsappSyndic} onChange={handleInputChange} placeholder="WhatsApp" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required/>
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="text-[10px] font-black text-slate-400 uppercase ml-1">CPF</label>
                        <input name="cpfSyndic" value={registerData.cpfSyndic} onChange={handleInputChange} placeholder="000.000.000-00" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500" required maxLength={14}/>
                      </div>
                      <div className="grid md:grid-cols-2 gap-4">
                        <div className="space-y-1 relative">
                          <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Senha de Acesso</label>
                          <div className="relative">
                            <input name="passwordSyndic" type={showPassword ? "text" : "password"} value={registerData.passwordSyndic} onChange={handleInputChange} placeholder="Senha" className="w-full p-3 border border-slate-200 rounded-xl outline-none focus:ring-2 focus:ring-emerald-500 pr-10" required/>
                            <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-3 text-slate-400 hover:text-emerald-600 transition-colors">{showPassword ? <EyeOff size={20}/> : <Eye size={20}/>}</button>
                          </div>
                        </div>
                        <div className="space-y-1">
                          <label className="text-[10px] font-black text-slate-400 uppercase ml-1">Confirmar Senha</label>
                          <input name="confirmPassword" type={showPassword ? "text" : "password"} value={registerData.confirmPassword} onChange={handleInputChange} placeholder="Repita a senha" className={`w-full p-3 border rounded-xl outline-none focus:ring-2 transition-all ${registerData.confirmPassword && registerData.passwordSyndic !== registerData.confirmPassword ? 'border-red-300 focus:ring-red-500 bg-red-50' : 'border-slate-200 focus:ring-emerald-500'}`} required/>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Termos e Botão */}
                  <div className="pt-4 border-t border-slate-100">
                    <div className="flex items-start gap-2 mb-4">
                      <input 
                        type="checkbox" 
                        id="terms" 
                        checked={termsAccepted} 
                        onChange={(e) => setTermsAccepted(e.target.checked)} 
                        className="mt-1 w-4 h-4 text-emerald-600 border-slate-300 rounded focus:ring-emerald-500 cursor-pointer"
                      />
                      <label htmlFor="terms" className="text-xs text-slate-500 cursor-pointer select-none">
                        Li e aceito os <button type="button" onClick={() => openModal('terms')} className="text-emerald-600 hover:underline font-bold">Termos de Uso</button> e a <button type="button" onClick={() => openModal('privacy')} className="text-emerald-600 hover:underline font-bold">Política de Privacidade</button>.
                      </label>
                    </div>

                    <button type="submit" disabled={loadingSubmit} className="w-full bg-emerald-600 text-white font-bold py-4 rounded-xl hover:bg-emerald-700 transition-all flex justify-center items-center gap-2 shadow-xl shadow-emerald-200 hover:shadow-2xl transform hover:-translate-y-1 disabled:opacity-70 disabled:cursor-not-allowed">
                      {loadingSubmit ? <Loader2 className="animate-spin"/> : (registerData.isTrial ? 'Finalizar Cadastro Grátis' : 'Ir para Pagamento Seguro')} 
                      {!loadingSubmit && <ArrowRight size={20}/>}
                    </button>
                    
                    <div className="mt-4 flex items-center justify-center gap-2 text-xs text-slate-400">
                      <Lock size={12} /> Ambiente 100% Seguro. Seus dados estão protegidos.
                    </div>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="py-10 px-4 max-w-6xl mx-auto">
        <header className="bg-[#022c22] py-8 text-center rounded-2xl mb-8 flex flex-col items-center justify-center">
          <div className="mb-4 scale-110"><Logo theme="light" /></div>
          <h1 className="text-3xl font-bold text-white mt-2">Planos Votzz</h1>
          <p className="text-slate-400">Transparência total para seu condomínio</p>
        </header>

        <div className="max-w-4xl mx-auto bg-[#064e3b] rounded-2xl p-6 border border-emerald-900 shadow-2xl mb-12">
          <div className="text-center mb-6">
            <h2 className="text-2xl font-bold text-white flex items-center justify-center gap-2">
              <Calculator className="text-emerald-500" /> Simule o valor para renovação
            </h2>
            <p className="text-slate-300 text-sm mt-2">
              O plano é selecionado automaticamente baseado no número de unidades.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-6 md:gap-10 items-end">
            <div>
              <label className="block text-emerald-300 font-semibold text-lg mb-2 flex items-center gap-2">
                <Building size={20} /> Quantas unidades (portas)?
              </label>
              <input
                type="number"
                min="1"
                value={units}
                onChange={(e) => setUnits(Math.max(1, parseInt(e.target.value) || 0))}
                className="w-full bg-[#022c22] text-white text-4xl font-black p-5 rounded-xl border-2 border-emerald-700 focus:border-emerald-400 outline-none text-center shadow-inner"
              />
            </div>

            <div>
              <div className="bg-[#022c22] p-1.5 rounded-xl flex border-2 border-emerald-700 shadow-inner min-h-[78px]">
                <button
                  onClick={() => setCycle('TRIMESTRAL')}
                  className={`flex-1 px-6 rounded-lg font-bold text-lg transition-all ${
                    cycle === 'TRIMESTRAL'
                      ? 'bg-emerald-700 text-white shadow-lg'
                      : 'text-emerald-300 hover:bg-emerald-900/60 hover:text-white'
                  }`}
                >
                  Trimestral
                </button>
                <button
                  onClick={() => setCycle('ANUAL')}
                  className={`flex-1 px-6 rounded-lg font-bold text-lg transition-all flex flex-col items-center justify-center ${
                    cycle === 'ANUAL'
                      ? 'bg-emerald-600 text-white shadow-lg'
                      : 'text-emerald-300 hover:bg-emerald-900/60 hover:text-white'
                  }`}
                >
                  Anual
                  <span className="text-xs uppercase tracking-wider bg-emerald-900 px-2 py-0.5 rounded mt-1 font-semibold">
                    20% OFF
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {['Essencial', 'Business', 'Custom'].map(plan => {
            const details = getPlanDetails(plan);
            const isActive = selectedPlanName === plan;

            let displayPrice = details.finalPrice;
            let rangeText = `${details.min} a ${details.max > 1000 ? 'Ilimitado' : details.max} unidades`;

            if (!isActive) {
              if (plan === 'Essencial') {
                displayPrice = cycle === 'TRIMESTRAL' ? 190.00 * 3 : 190.00 * 12 * 0.8;
                rangeText = "Até 30 unidades";
              } else if (plan === 'Business') {
                displayPrice = cycle === 'TRIMESTRAL' ? 349.00 * 3 : 349.00 * 12 * 0.8;
                rangeText = "31 a 80 unidades";
              } else {
                displayPrice = 0;
                rangeText = "Acima de 80 unidades";
              }
            }

            return (
              <div key={plan} className={`p-8 rounded-2xl transition-all border ${getCardClasses(plan)}`}>
                
                {/* Destaque 30 Dias Grátis */}
                <div className="absolute top-0 right-0 p-4">
                  <div className="bg-emerald-500 text-white text-[10px] font-bold px-2 py-1 rounded-full uppercase tracking-wider shadow-md">
                     30 Dias Grátis
                  </div>
                </div>

                <div>
                  <h3 className="text-2xl font-bold flex items-center gap-2">
                    {plan}
                    {plan === 'Custom' && <Crown className="text-yellow-400" size={28} />}
                  </h3>
                  <p className="text-sm opacity-80 mb-4">{rangeText}</p>
                  
                  {/* Lógica de Preço (Riscado + Mensal) */}
                  <div className="mt-4">
                    {cycle === 'ANUAL' && (
                        <div className="text-sm line-through opacity-50 font-bold decoration-red-500">
                           {Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(details.fullPrice)}
                        </div>
                    )}
                    
                    <div className="text-3xl font-black">
                      {plan === 'Custom' && !isActive 
                        ? 'R$ ---' 
                        : Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(details.finalPrice)
                      }
                    </div>

                    <div className="flex items-center gap-2 mt-1">
                       <TrendingDown size={14} className="text-emerald-500" />
                       <span className="text-sm text-emerald-500 font-bold">
                         {plan === 'Custom' && !isActive ? '' : `Sai a ${Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(details.monthlyEquivalent)} /mês`}
                       </span>
                    </div>
                  </div>

                  <p className="text-xs opacity-60 mb-6 uppercase mt-2">{cycle}</p>
                  
                  <BenefitsList isDark={isActive} />
                </div>
                
                <div className="mt-auto space-y-3">
                  {isActive ? (
                    <>
                      <button 
                        onClick={() => handleSelectPlan(plan, false)}
                        className="w-full bg-emerald-600 text-white py-3 rounded-lg font-bold hover:bg-emerald-700 transition-colors"
                      >
                        Contratar Agora
                      </button>
                      <button 
                        onClick={() => handleSelectPlan(plan, true)}
                        className="w-full mt-2 text-emerald-600 text-sm font-bold py-2 hover:bg-emerald-50 rounded-lg border-2 border-emerald-500/20 transition-all flex items-center justify-center gap-2"
                      >
                        <Sparkles size={16} /> Testar 30 Dias Grátis
                      </button>
                    </>
                  ) : (
                    <>
                      <button 
                        disabled
                        className="w-full bg-emerald-600/50 text-white/70 py-3 rounded-lg font-bold cursor-not-allowed"
                      >
                        Contratar Agora
                      </button>
                      <button 
                        disabled
                        className="w-full mt-2 text-emerald-600/50 text-sm font-bold py-2 bg-transparent border-none cursor-not-allowed"
                      >
                        Testar 30 Dias Grátis
                      </button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
        
        {cycle === 'ANUAL' && (
          <div className="text-center mt-12 bg-emerald-100 max-w-lg mx-auto p-4 rounded-xl border border-emerald-200 text-emerald-800 font-medium shadow-sm flex items-center justify-center gap-2">
            <TrendingDown size={20} />
            <span>Você está economizando 20% escolhendo o plano anual!</span>
          </div>
        )}
      </div>

      <style>{`
        .pricing-card { border-radius: 1.5rem; padding: 2rem; transition: all 0.3s ease; display: flex; flex-direction: column; min-height: 800px; }
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: #f1f5f9; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
      `}</style>
    </div>
  );
}
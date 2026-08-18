import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import api from '../../services/api';
import { Logo } from '../../components/Logo';
import { 
  Building, User, ArrowRight, ArrowLeft, MapPin, AlertCircle, CreditCard, Eye, EyeOff, Copy, Check, Ticket, CheckCircle, Loader2, Gift // <--- Gift IMPORTADO AQUI
} from 'lucide-react';

const CUSTOM_TRIMESTRAL_ID = '33333333-3333-3333-3333-333333333333';
const CUSTOM_ANUAL_ID      = '44444444-4444-4444-4444-444444444444';

export default function CondoRegister() {
  const navigate = useNavigate();
  const location = useLocation();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  // Flag que vem do Pricing
  const isTrial = location.state?.isTrial === true;

  // UI States
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [pixData, setPixData] = useState<{payload: string, image: string} | null>(null);
  const [copied, setCopied] = useState(false);
  const [successFree, setSuccessFree] = useState(false);

  // States do Cupom
  const [discountPercent, setDiscountPercent] = useState(0);
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponMessage, setCouponMessage] = useState({ type: '', text: '' });

  // Form
  const [formData, setFormData] = useState({
    planId: location.state?.planId || '11111111-1111-1111-1111-111111111111',
    condoName: '', cnpj: '', qtyUnits: location.state?.preFilledUnits || '', qtyBlocks: '', secretKeyword: '',
    nameSyndic: '', emailSyndic: '', cpfSyndic: '', whatsappSyndic: '', passwordSyndic: '', confirm: '',
    cep: '', logradouro: '', numero: '', bairro: '', cidade: '', estado: '', pontoReferencia: '',
    couponCode: ''
  });

  const isCustom = formData.planId === CUSTOM_TRIMESTRAL_ID || formData.planId === CUSTOM_ANUAL_ID;

  // Lógica de Cálculo de Preço (Base)
  const calculateBasePrice = () => {
    if (!isCustom) return 0;
    const units = Number(formData.qtyUnits);
    if (!units) return 0;

    let monthly = 490.00;
    if (units > 80) {
      monthly += (units - 80) * 2.50;
    }

    if (formData.planId === CUSTOM_TRIMESTRAL_ID) {
      return monthly * 3;
    } else {
      return (monthly * 12) * 0.80; 
    }
  };

  const basePrice = calculateBasePrice();
  const finalPrice = discountPercent > 0 ? basePrice - (basePrice * (discountPercent / 100)) : basePrice;

  const handleApplyCoupon = async () => {
      if (!formData.couponCode) return;
      setCouponLoading(true);
      setCouponMessage({ type: '', text: '' });
      setDiscountPercent(0);

      try {
          const res = await api.get(`/auth/validate-coupon?code=${formData.couponCode}`);
          const percent = res.data; 
          setDiscountPercent(percent);
          setCouponMessage({ type: 'success', text: `Cupom aplicado! ${percent}% OFF` });
      } catch (err: any) {
          setDiscountPercent(0);
          const msg = err.response?.data?.message || 'Cupom inválido';
          setCouponMessage({ type: 'error', text: msg });
      } finally {
          setCouponLoading(false);
      }
  };

  const toggleCustomCycle = (cycle: 'TRIMESTRAL' | 'ANUAL') => {
    setFormData(prev => ({
        ...prev,
        planId: cycle === 'ANUAL' ? CUSTOM_ANUAL_ID : CUSTOM_TRIMESTRAL_ID
    }));
  };

  const fetchCep = async (cep: string) => {
      const cleanCep = cep.replace(/\D/g, '');
      if(cleanCep.length !== 8) return;
      try {
          const res = await fetch(`https://viacep.com.br/ws/${cleanCep}/json/`);
          const data = await res.json();
          if(!data.erro) {
              setFormData(prev => ({ 
                  ...prev, 
                  logradouro: data.logradouro, 
                  bairro: data.bairro, 
                  cidade: data.localidade, 
                  estado: data.uf 
              }));
              document.getElementById('numeroInput')?.focus();
          }
      } catch(e) {}
  };

  const handleChange = (e: any) => {
    setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }));
    if(e.target.name === 'cep') fetchCep(e.target.value);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if(formData.passwordSyndic !== formData.confirm) return setError("As senhas não conferem!");
    
    setLoading(true);
    setError('');

    try {
      const res = await api.post('/auth/register-condo', {
          ...formData,
          qtyUnits: Number(formData.qtyUnits),
          qtyBlocks: Number(formData.qtyBlocks),
          isTrial: isTrial // Envia para o backend que é Trial (se for o caso)
      });
      
      // Se for Trial, o backend deve retornar sucesso sem redirectUrl/pix
      const { redirectUrl, pixPayload, pixImage } = res.data;

      if (isTrial) {
          setSuccessFree(true);
          setStep(3);
      } else if (pixPayload) {
          setPixData({ payload: pixPayload, image: pixImage });
          setStep(3);
      } else if (redirectUrl) {
          window.location.href = redirectUrl;
      } else {
          // Fallback se não retornou nada específico mas deu 200 OK
          setSuccessFree(true);
          setStep(3);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Erro ao registrar.';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  const handleCopyPix = () => {
      if(pixData?.payload) {
          navigator.clipboard.writeText(pixData.payload);
          setCopied(true);
          setTimeout(() => setCopied(false), 2000);
      }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center p-4">
      <div className="mb-8"><Link to="/"><Logo theme="dark" /></Link></div>
      <div className="w-full max-w-3xl bg-slate-800 rounded-2xl shadow-2xl border border-slate-700 overflow-hidden">
        
        {/* Header */}
        <div className="bg-slate-900 p-4 border-b border-slate-700 flex justify-center gap-4">
           <div className={`flex items-center gap-2 ${step >= 1 ? 'text-emerald-500' : 'text-slate-500'}`}><div className="w-8 h-8 rounded-full border-2 flex items-center justify-center font-bold">1</div><span>Dados</span></div>
           <div className="w-10 h-px bg-slate-700 self-center"></div>
           <div className={`flex items-center gap-2 ${step >= 2 ? 'text-emerald-500' : 'text-slate-500'}`}><div className="w-8 h-8 rounded-full border-2 flex items-center justify-center font-bold">2</div><span>{isTrial ? 'Confirmação' : 'Pagamento'}</span></div>
           
           {/* Mostra passo 3 se não for Trial ou se já tiver finalizado */}
           {(!isTrial || successFree) && (
               <>
                   <div className="w-10 h-px bg-slate-700 self-center"></div>
                   <div className={`flex items-center gap-2 ${step >= 3 ? 'text-emerald-500' : 'text-slate-500'}`}>
                       <div className="w-8 h-8 rounded-full border-2 flex items-center justify-center font-bold">3</div>
                       <span>Concluído</span>
                   </div>
               </>
           )}
        </div>

        <form onSubmit={handleSubmit} className="p-6 md:p-8 space-y-6">
          {error && <div className="bg-red-500/10 text-red-200 p-4 rounded-lg flex gap-2 items-center border border-red-500/30"><AlertCircle size={20}/> {error}</div>}

          {step === 1 && (
            <div className="space-y-6 animate-in slide-in-from-right-4">
              {/* --- STEP 1: DADOS DO CONDOMÍNIO E ENDEREÇO --- */}
              <div className="bg-slate-700/30 p-5 rounded-xl border border-slate-600">
                <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2"><Building className="text-emerald-500"/> Dados do Condomínio</h3>
                <div className="grid md:grid-cols-2 gap-4">
                    <input required name="condoName" placeholder="Nome do Condomínio" className="input" onChange={handleChange} value={formData.condoName} />
                    <input required name="cnpj" placeholder="CNPJ" className="input" onChange={handleChange} value={formData.cnpj} />
                    <div className="grid grid-cols-2 gap-2">
                        <input required type="number" name="qtyUnits" placeholder="Unidades" className="input" onChange={handleChange} value={formData.qtyUnits} />
                        <input required type="number" name="qtyBlocks" placeholder="Blocos" className="input" onChange={handleChange} value={formData.qtyBlocks} />
                    </div>
                    <input required name="secretKeyword" placeholder="Palavra-Passe (Moradores)" className="input border-emerald-500/50 focus:border-emerald-400" onChange={handleChange} value={formData.secretKeyword} />
                </div>
              </div>

              <div className="bg-slate-700/30 p-5 rounded-xl border border-slate-600 relative">
                <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2"><MapPin className="text-blue-500"/> Endereço</h3>
                <div className="grid grid-cols-3 gap-3">
                    <input required name="cep" placeholder="CEP" maxLength={9} className="input" onChange={handleChange} value={formData.cep} />
                    <input required name="logradouro" placeholder="Rua / Av" className="input col-span-2" onChange={handleChange} value={formData.logradouro} />
                </div>
                <div className="grid grid-cols-3 gap-3 mt-3">
                    <input required id="numeroInput" name="numero" placeholder="Nº" className="input" onChange={handleChange} value={formData.numero} />
                    <input required name="bairro" placeholder="Bairro" className="input" onChange={handleChange} value={formData.bairro} />
                    <input required name="cidade" placeholder="Cidade" className="input" onChange={handleChange} value={formData.cidade} />
                </div>
                <div className="grid grid-cols-2 gap-3 mt-3">
                    <input required name="estado" placeholder="UF" maxLength={2} className="input" onChange={handleChange} value={formData.estado} />
                    <input name="pontoReferencia" placeholder="Referência (Opcional)" className="input" onChange={handleChange} value={formData.pontoReferencia} />
                </div>
              </div>
              <div className="flex justify-end">
                <button type="button" onClick={() => { if(formData.condoName && formData.qtyUnits && formData.qtyBlocks && formData.cep) setStep(2); else setError("Preencha todos os campos da etapa 1."); }} className="btn-primary flex items-center gap-2">Próximo <ArrowRight size={18}/></button>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-6 animate-in slide-in-from-right-4">
               {/* --- STEP 2: DADOS DO SÍNDICO --- */}
               <div className="bg-slate-700/30 p-5 rounded-xl border border-slate-600">
                <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2"><User className="text-emerald-500"/> Dados do Síndico</h3>
                <div className="space-y-4">
                    <input required name="nameSyndic" placeholder="Nome Completo" className="input" onChange={handleChange} value={formData.nameSyndic} />
                    <div className="grid md:grid-cols-2 gap-4">
                        <input required name="cpfSyndic" placeholder="CPF" className="input" onChange={handleChange} value={formData.cpfSyndic} />
                        <input required name="whatsappSyndic" placeholder="WhatsApp" className="input" onChange={handleChange} value={formData.whatsappSyndic} />
                    </div>
                    <input required type="email" name="emailSyndic" placeholder="E-mail de Acesso (Login)" className="input" onChange={handleChange} value={formData.emailSyndic} />
                    
                    <div className="grid md:grid-cols-2 gap-4">
                        <div className="relative">
                            <input required type={showPassword ? "text" : "password"} name="passwordSyndic" placeholder="Senha" className="input pr-10" onChange={handleChange} value={formData.passwordSyndic} />
                            <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-3 text-slate-400 hover:text-white transition-colors">{showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button>
                        </div>
                        <div className="relative">
                            <input required type={showConfirmPassword ? "text" : "password"} name="confirm" placeholder="Confirmar Senha" className="input pr-10" onChange={handleChange} value={formData.confirm} />
                            <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute right-3 top-3 text-slate-400 hover:text-white transition-colors">{showConfirmPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button>
                        </div>
                    </div>
                </div>
               </div>
               
               {/* --- ÁREA DE PAGAMENTO (Só exibe se NÃO for Trial e se for Custom) --- */}
               {!isTrial && isCustom && (
                   <>
                       <div className="bg-slate-700/30 p-4 rounded-xl border border-slate-600 mb-4">
                           <label className="text-sm text-slate-400 mb-1 block flex items-center gap-2">
                               <Ticket size={16} /> Possui um Cupom de Desconto?
                           </label>
                           <div className="flex gap-2">
                               <input 
                                   name="couponCode" 
                                   placeholder="Código" 
                                   className="input border-dashed border-emerald-500/30 focus:border-emerald-500 uppercase font-mono tracking-wider flex-1"
                                   onChange={(e) => {
                                       handleChange(e);
                                       if(discountPercent > 0) {
                                            setDiscountPercent(0);
                                            setCouponMessage({type: '', text: ''});
                                       }
                                   }} 
                                   value={formData.couponCode} 
                               />
                               <button 
                                   type="button" 
                                   onClick={handleApplyCoupon}
                                   disabled={couponLoading || !formData.couponCode}
                                   className="bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-700 text-white px-4 rounded-lg font-bold text-sm transition-colors flex items-center gap-2"
                               >
                                   {couponLoading ? <Loader2 className="animate-spin" size={16} /> : 'Aplicar'}
                               </button>
                           </div>
                           
                           {couponMessage.text && (
                               <p className={`text-xs mt-2 flex items-center gap-1 ${couponMessage.type === 'success' ? 'text-emerald-400' : 'text-red-400'}`}>
                                   {couponMessage.type === 'success' ? <CheckCircle size={12} /> : <AlertCircle size={12} />}
                                   {couponMessage.text}
                               </p>
                           )}
                       </div>

                       <div className="bg-slate-700/50 p-6 rounded-xl border border-emerald-500/50 text-center relative overflow-hidden">
                           <div className="absolute top-4 right-4 flex bg-slate-800 rounded-lg p-1 border border-slate-600">
                               <button 
                                 type="button"
                                 onClick={() => toggleCustomCycle('TRIMESTRAL')}
                                 className={`px-3 py-1 text-xs font-bold rounded ${formData.planId === CUSTOM_TRIMESTRAL_ID ? 'bg-emerald-500 text-white' : 'text-slate-400 hover:text-white'}`}
                               >
                                 Trimestral
                               </button>
                               <button 
                                 type="button"
                                 onClick={() => toggleCustomCycle('ANUAL')}
                                 className={`px-3 py-1 text-xs font-bold rounded ${formData.planId === CUSTOM_ANUAL_ID ? 'bg-emerald-500 text-white' : 'text-slate-400 hover:text-white'}`}
                               >
                                 Anual (-20%)
                               </button>
                           </div>

                           <p className="text-slate-400 text-sm uppercase font-bold tracking-widest mb-2 mt-4">Resumo do Plano Custom</p>
                           
                           {discountPercent > 0 ? (
                               <div className="flex flex-col items-center">
                                   <span className="text-slate-500 text-lg line-through decoration-red-500 decoration-2">
                                       {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(basePrice)}
                                   </span>
                                   <p className="text-4xl font-black text-emerald-400 transition-all duration-300">
                                     {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(finalPrice)}
                                   </p>
                                   <span className="text-emerald-500 text-xs font-bold bg-emerald-500/10 px-2 py-0.5 rounded mt-1">
                                       {discountPercent}% DE DESCONTO APLICADO
                                   </span>
                               </div>
                           ) : (
                               <p className="text-4xl font-black text-white transition-all duration-300">
                                 {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(basePrice)}
                               </p>
                           )}

                           <p className="text-slate-400 text-xs mt-2">
                               Valor {formData.planId === CUSTOM_ANUAL_ID ? 'Anual' : 'Trimestral'} estimado para {formData.qtyUnits} unidades.
                           </p>
                       </div>
                   </>
               )}
               
               {!isTrial && !isCustom && (
                   <div className="bg-emerald-900/20 p-6 rounded-xl border border-emerald-500/30 text-center">
                       <CreditCard className="mx-auto text-emerald-400 mb-2" size={32} />
                       <h4 className="text-white font-bold text-lg">Checkout Seguro</h4>
                       <p className="text-emerald-300 text-sm mt-1">Você será redirecionado para o Kiwify para concluir o pagamento.</p>
                   </div>
               )}

               {isTrial && (
                   <div className="bg-emerald-900/20 p-6 rounded-xl border border-emerald-500/30 text-center">
                       <Gift className="mx-auto text-emerald-400 mb-2" size={32} />
                       <h4 className="text-white font-bold text-lg">Período de Teste Grátis</h4>
                       <p className="text-emerald-300 text-sm mt-1">Você terá 30 dias de acesso completo sem custo algum.</p>
                   </div>
               )}

               <div className="flex gap-4">
                 <button type="button" onClick={() => setStep(1)} className="px-6 py-3 rounded-lg text-slate-400 hover:bg-slate-800 transition-colors"><ArrowLeft className="inline mr-2"/> Voltar</button>
                 <button type="submit" disabled={loading} className="flex-1 btn-primary py-4 text-lg shadow-lg shadow-emerald-900/50">
                    {loading ? 'Processando...' : (isTrial ? 'Finalizar Cadastro Grátis' : (isCustom ? 'Gerar Pix' : 'Ir para Pagamento'))}
                 </button>
               </div>
            </div>
          )}
          
          {/* --- STEP 3: SUCESSO OU PIX --- */}
          {step === 3 && (
            successFree ? (
              <div className="space-y-6 animate-in slide-in-from-right-4 text-center">
                  <div className="bg-emerald-500/10 p-8 rounded-2xl inline-block border border-emerald-500/30">
                      <CheckCircle className="w-24 h-24 mx-auto text-emerald-500 mb-4" />
                      <h2 className="text-2xl font-bold text-white">Cadastro Concluído!</h2>
                      <p className="text-slate-300 mt-2">{isTrial ? "Seu período de 30 dias grátis começou." : "Pagamento confirmado ou processado."}</p>
                  </div>
                  <Link to="/login" className="btn-primary inline-block w-full py-4 mt-4">Acessar Sistema</Link>
              </div>
            ) : pixData && (
              <div className="space-y-6 animate-in slide-in-from-right-4 text-center">
                  <div className="bg-white p-8 rounded-2xl inline-block">
                      <img src={`data:image/png;base64,${pixData.image}`} alt="QR Code Pix" className="w-48 h-48 mx-auto" />
                  </div>
                  <div className="max-w-md mx-auto">
                      <p className="text-white font-bold mb-2">Código Copia e Cola</p>
                      <div className="flex gap-2">
                          <input readOnly value={pixData.payload} className="input text-xs font-mono" />
                          <button type="button" onClick={handleCopyPix} className="bg-emerald-600 text-white p-3 rounded-lg">
                              {copied ? <Check size={20}/> : <Copy size={20}/>}
                          </button>
                      </div>
                  </div>
                  <p className="text-slate-400 text-sm">Após o pagamento, seu acesso será liberado automaticamente em instantes.</p>
                  <Link to="/login" className="btn-primary inline-block w-full py-4 mt-4">Já Paguei (Ir para Login)</Link>
              </div>
            )
          )}
        </form>
      </div>
      <style>{`
        .input { width: 100%; background-color: #1e293b; color: white; border: 1px solid #334155; border-radius: 0.5rem; padding: 0.75rem 1rem; outline: none; transition: 0.2s; }
        .input:focus { border-color: #10b981; box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2); }
        .btn-primary { background-color: #10b981; color: white; font-weight: bold; border-radius: 0.5rem; padding: 0.75rem 1.5rem; transition: 0.2s; text-align:center; }
        .btn-primary:hover { background-color: #059669; }
        .btn-primary:disabled { opacity: 0.7; cursor: not-allowed; }
      `}</style>
    </div>
  );
}
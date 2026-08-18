import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, CreditCard, Shield, ArrowLeft, Building, Crown, AlertTriangle, Calendar } from 'lucide-react';
import api from '../../services/api';

const SubscriptionRenovation: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  
  // Estado para controlar se deve mostrar o componente
  const [needsRenewal, setNeedsRenewal] = useState(false);
  const [daysRemaining, setDaysRemaining] = useState<number>(0);
  const [currentPlanName, setCurrentPlanName] = useState('');
  
  // Estado do Formulário
  const [units, setUnits] = useState<number>(50); 
  const [cycle, setCycle] = useState<'TRIMESTRAL' | 'ANUAL'>('ANUAL');
  const [calculatedPrice, setCalculatedPrice] = useState(0);

  // 1. Ao carregar, verifica com o Backend se precisa mostrar
  useEffect(() => {
    const checkStatus = async () => {
      try {
        const res = await api.get('/subscription/status');
        const { needsRenewal, daysRemaining, currentPlan, units } = res.data;
        
        if (!needsRenewal) {
          setNeedsRenewal(false); // Não mostra nada se estiver longe do vencimento
        } else {
          setNeedsRenewal(true);
          setDaysRemaining(daysRemaining);
          setCurrentPlanName(currentPlan);
          setUnits(units); // Preenche com as unidades atuais do banco
        }
      } catch (err) {
        console.error("Erro ao verificar status da assinatura");
      } finally {
        setLoading(false);
      }
    };
    checkStatus();
  }, []);

  // 2. Calcula preço localmente apenas para visualização (Backend valida depois)
  useEffect(() => {
    let price = 0;
    const isCustom = units > 80 || currentPlanName.toLowerCase().includes('custom');
    
    // Valores Base
    const baseMensalEssencial = 190.00;
    const baseMensalBusiness = 349.00;

    let baseMensal = 0;

    if (currentPlanName.toLowerCase().includes('essencial') && units <= 30) {
        baseMensal = baseMensalEssencial;
    } else {
        // Business ou Custom base
        baseMensal = baseMensalBusiness;
        if (units > 80) {
            baseMensal += (units - 80) * 1.50;
        }
    }

    if (cycle === 'TRIMESTRAL') {
        price = baseMensal * 3;
    } else {
        price = (baseMensal * 12) * 0.8; // 20% desconto
    }
    
    setCalculatedPrice(price);
  }, [units, cycle, currentPlanName]);

  const handleRenovation = async () => {
    setProcessing(true);
    try {
        // Pede ao backend o link correto (Kiwify ou Asaas)
        const response = await api.post('/subscription/renew', {
            cycle: cycle,
            units: units
        });

        const { paymentUrl, gateway } = response.data;

        if (paymentUrl) {
            if (gateway === 'KIWIFY') {
                // Abre em nova aba para Kiwify
                window.open(paymentUrl, '_blank');
                alert("Redirecionando para pagamento seguro no Kiwify...");
            } else {
                // Redireciona na mesma aba para Fatura Asaas
                window.location.href = paymentUrl;
            }
        } else {
            alert("Erro ao gerar link de renovação.");
        }
    } catch (error) {
        console.error("Erro:", error);
        alert("Não foi possível processar a renovação agora.");
    } finally {
        setProcessing(false);
    }
  };

  // Se estiver carregando ou não precisar renovar, não mostra nada (ou retorna null)
  if (loading) return null; // Ou um spinner pequeno
  if (!needsRenewal) return null; // O componente "some" se não precisar renovar

  // Cor do alerta baseada na urgência
  const alertColor = daysRemaining <= 7 ? "bg-red-100 text-red-700 border-red-200" : "bg-amber-50 text-amber-700 border-amber-200";

  return (
    <div className="fixed bottom-0 right-0 p-4 md:p-6 z-50 animate-in slide-in-from-bottom-10 fade-in duration-500">
        {/* Este componente pode ser um Modal Full Screen ou um Widget Flutuante. 
            Abaixo fiz estilo "Modal/Card Centralizado" para chamar atenção */}
        
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-white w-full max-w-4xl rounded-2xl shadow-2xl overflow-hidden flex flex-col md:flex-row">
                
                {/* Lado Esquerdo - Info */}
                <div className="bg-slate-900 p-8 text-white md:w-2/5 flex flex-col justify-between">
                    <div>
                        <div className="flex items-center gap-2 text-amber-400 font-bold mb-6">
                            <AlertTriangle /> Atenção Necessária
                        </div>
                        <h2 className="text-3xl font-bold mb-2">Renove seu Acesso</h2>
                        <p className="text-slate-400 text-sm">
                            Sua assinatura do plano <strong>{currentPlanName}</strong> vence em breve. Garanta a continuidade do serviço.
                        </p>
                    </div>

                    <div className={`mt-6 p-4 rounded-xl border ${alertColor}`}>
                        <div className="flex items-center gap-3">
                            <Calendar size={24} />
                            <div>
                                <p className="text-xs font-bold uppercase opacity-80">Vence em</p>
                                <p className="text-2xl font-black">{daysRemaining} dias</p>
                            </div>
                        </div>
                    </div>

                    <button onClick={() => navigate('/dashboard')} className="mt-8 text-slate-500 text-sm hover:text-white transition-colors flex items-center gap-2">
                        <ArrowLeft size={16}/> Resolver depois
                    </button>
                </div>

                {/* Lado Direito - Opções */}
                <div className="p-8 md:w-3/5 bg-slate-50">
                    <div className="space-y-6">
                        
                        {/* Seletor de Unidades (Só habilita se for Custom, opcional) */}
                        <div>
                            <label className="text-xs font-bold text-slate-500 uppercase">Unidades Atuais</label>
                            <div className="flex items-center gap-2 bg-white p-3 rounded-xl border border-slate-200 mt-1">
                                <Building className="text-emerald-500" />
                                <input 
                                    type="number" 
                                    value={units}
                                    onChange={(e) => setUnits(Number(e.target.value))}
                                    className="w-full outline-none font-bold text-slate-700 bg-transparent"
                                    // Se for plano fixo, talvez queira bloquear edição: disabled={currentPlanName !== 'Custom'}
                                />
                            </div>
                        </div>

                        {/* Ciclo */}
                        <div>
                            <label className="text-xs font-bold text-slate-500 uppercase">Escolha o Período</label>
                            <div className="grid grid-cols-2 gap-3 mt-1">
                                <button 
                                    onClick={() => setCycle('TRIMESTRAL')}
                                    className={`p-3 rounded-xl border-2 font-bold text-sm transition-all ${cycle === 'TRIMESTRAL' ? 'border-emerald-500 bg-white text-emerald-700' : 'border-transparent bg-slate-200 text-slate-500'}`}
                                >
                                    Trimestral
                                </button>
                                <button 
                                    onClick={() => setCycle('ANUAL')}
                                    className={`p-3 rounded-xl border-2 font-bold text-sm transition-all relative ${cycle === 'ANUAL' ? 'border-emerald-500 bg-white text-emerald-700' : 'border-transparent bg-slate-200 text-slate-500'}`}
                                >
                                    Anual
                                    <span className="absolute -top-2 -right-2 bg-emerald-500 text-white text-[10px] px-2 py-0.5 rounded-full">20% OFF</span>
                                </button>
                            </div>
                        </div>

                        {/* Total e Botão */}
                        <div className="pt-6 border-t border-slate-200">
                            <div className="flex justify-between items-end mb-4">
                                <span className="text-slate-500 font-medium">Total a pagar</span>
                                <span className="text-3xl font-black text-slate-800">
                                    {Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(calculatedPrice)}
                                </span>
                            </div>

                            <button 
                                onClick={handleRenovation}
                                disabled={processing}
                                className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-4 rounded-xl shadow-lg shadow-emerald-200 transition-all flex items-center justify-center gap-2"
                            >
                                {processing ? <span className="animate-pulse">Gerando Fatura...</span> : (
                                    <>
                                        <CreditCard size={20} />
                                        Renovar Assinatura Agora
                                    </>
                                )}
                            </button>
                            
                            <p className="text-center text-[10px] text-slate-400 mt-3">
                                {currentPlanName.includes('Custom') || units > 80 
                                    ? 'Pagamento processado via Asaas (Pix/Boleto/Cartão)' 
                                    : 'Pagamento processado via Kiwify (Checkout Seguro)'}
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
  );
};

export default SubscriptionRenovation;
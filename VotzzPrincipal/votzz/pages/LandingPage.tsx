import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Menu, X, CheckCircle, Clock, DollarSign, Smartphone, FileText, Users, Lock, 
  ChevronRight, Star, BarChart3, Mail, ShieldCheck, TrendingUp, Gift, 
  LayoutDashboard, Zap, Check, Headphones, Crown, Video, ArrowRight, 
  MousePointerClick, Percent, CircleDollarSign, Handshake, Quote,
  Megaphone, CalendarRange, Package, Wrench, Scale, Building, Bell, Search, 
  MessageSquare, ChevronDown, Plus, FileSignature, AlertCircle, UserCheck, QrCode
} from 'lucide-react';
import { Logo } from '../components/Logo';
import { User } from '../types';

/**
 * LANDING PAGE DEFINITIVA - VOTZZ
 * Posicionamento: O Sistema Operacional do Condomínio.
 * Destaque: Mockup Hero Interativo simulando a UI real do sistema em código (Alinhamento Reto).
 */

interface LandingPageProps {
  user: User | null;
}

const LandingPage: React.FC<LandingPageProps> = ({ user }) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [activeFaq, setActiveFaq] = useState<number | null>(null);
  
  // ESTADO PARA O MOCKUP INTERATIVO DO HERO
  const [activeMockupTab, setActiveMockupTab] = useState('dashboard');
  
  const navigate = useNavigate();

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const scrollToSection = (id: string) => {
    const element = document.getElementById(id);
    if (element) {
      const offset = 80;
      const bodyRect = document.body.getBoundingClientRect().top;
      const elementRect = element.getBoundingClientRect().top;
      const elementPosition = elementRect - bodyRect;
      const offsetPosition = elementPosition - offset;

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth'
      });
      setMobileMenuOpen(false);
    }
  };

  const benefitsList = [
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

  const generalFaq = [
    {
      q: "Moradores mais idosos conseguem usar o Votzz?",
      a: "Sim! Nossa interface foi desenhada com foco em acessibilidade. Em caso de dúvidas, o síndico pode registrar os votos e solicitações manualmente pelo painel administrativo, garantindo que ninguém fique de fora."
    },
    {
      q: "Como funciona o período de teste de 30 dias?",
      a: "Você cria sua conta e tem acesso ilimitado a todos os recursos da plataforma, incluindo assembleias online e gestão de chamados. Não pedimos cartão de crédito no cadastro. Se não gostar, basta não assinar após os 30 dias."
    },
    {
      q: "A votação online do Votzz tem validade jurídica?",
      a: "Absolutamente. O Votzz utiliza registros com auditoria de IP, data, hora e autenticação de duplo fator (quando ativada), gerando atas automáticas em conformidade com o Código Civil brasileiro e a LGPD."
    },
    {
      q: "Posso usar apenas para a Assembleia e não para chamados?",
      a: "Sim. A plataforma é modular. Você pode usar ou não o módulo de 'Encomendas' ou 'Reservas' se o seu condomínio não precisar, mantendo a interface limpa apenas com o que você realmente utiliza."
    }
  ];

  const affiliateFaq = [
    {
      q: "Como recebo minha comissão de 30%?",
      a: "O pagamento é feito via PIX todo dia 10 de cada mês, referente às vendas confirmadas e pagas no mês anterior através do seu link exclusivo."
    },
    {
      q: "A comissão é recorrente?",
      a: "Não. Você recebe 30% do valor total do plano (trimestral ou anual) contratado pelo cliente logo na primeira fatura paga."
    },
    {
      q: "Preciso ser síndico para ser afiliado?",
      a: "Não. O programa é aberto para síndicos profissionais, administradoras, consultores de condomínio, corretores ou qualquer pessoa com networking na área."
    }
  ];

  const testimonials = [
    {
      name: "Mariana Costa",
      role: "Síndica Profissional - SP",
      text: "O Votzz mudou a rotina dos meus condomínios. Deixamos de usar 4 planilhas diferentes. Agora reservas, convidados e assembleias ficam em um único lugar.",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Mariana"
    },
    {
      name: "Ricardo Mendes",
      role: "Conselheiro - Residencial Aurora",
      text: "A transparência que a plataforma trouxe acabou com as brigas no WhatsApp. Acompanhar os chamados de manutenção em tempo real deu muita paz ao conselho.",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Ricardo"
    },
    {
      name: "Fernanda Oliveira",
      role: "Administradora de Condomínios",
      text: "Implementei em 5 condomínios da minha carteira. A interface é tão simples que os moradores centralizaram toda a comunicação no app naturalmente.",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Fernanda"
    }
  ];

  return (
    <div className="min-h-screen bg-white font-sans text-slate-800 antialiased selection:bg-emerald-200 selection:text-emerald-900 overflow-x-hidden">
      
      {/* =========================================================
          HEADER
          ========================================================= */}
      <header 
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          isScrolled ? 'bg-white/90 backdrop-blur-md shadow-sm py-3 border-b border-slate-100' : 'bg-white/50 backdrop-blur-sm py-5'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <Link to="/" className="cursor-pointer hover:opacity-80 transition-opacity" onClick={() => scrollToSection('home')}>
            <Logo theme="dark" />
          </Link>

          <nav className="hidden md:flex items-center space-x-8">
            <button onClick={() => scrollToSection('plataforma')} className="text-sm font-semibold text-slate-600 hover:text-emerald-600 transition-colors">A Plataforma</button>
            <button onClick={() => scrollToSection('como-funciona')} className="text-sm font-semibold text-slate-600 hover:text-emerald-600 transition-colors">Como Funciona</button>
            <button onClick={() => navigate('/pricing')} className="text-sm font-semibold text-slate-600 hover:text-emerald-600 transition-colors">Planos</button>
            
            <button 
              onClick={() => scrollToSection('affiliates')} 
              className="text-sm font-bold text-slate-900 flex items-center gap-1.5 hover:text-emerald-600 transition-colors group outline-none bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-100"
            >
              <TrendingUp className="w-4 h-4 text-emerald-500 group-hover:scale-110 transition-transform" />
              Parceiros 30%
            </button>
          </nav>

          <div className="hidden md:flex items-center space-x-5">
            {user ? (
              <Link 
                to="/dashboard" 
                className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-2.5 rounded-full text-sm font-bold transition-all shadow-md hover:shadow-lg transform hover:-translate-y-0.5 flex items-center gap-2"
              >
                <LayoutDashboard className="w-4 h-4" />
                Painel do Síndico
              </Link>
            ) : (
              <>
                <Link to="/login" className="text-sm font-bold text-slate-600 hover:text-emerald-600 transition-colors">Entrar</Link>
                <button 
                  onClick={() => navigate('/pricing')}
                  className="bg-slate-900 hover:bg-emerald-600 text-white px-6 py-2.5 rounded-full text-sm font-bold transition-all shadow-md transform hover:-translate-y-0.5"
                >
                  Solicitar Demo
                </button>
              </>
            )}
          </div>

          <button 
            className="md:hidden text-slate-900 p-2 rounded-lg hover:bg-slate-100 transition-colors"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <X /> : <Menu />}
          </button>
        </div>

        {/* Menu Mobile */}
        {mobileMenuOpen && (
          <div className="md:hidden absolute top-full left-0 right-0 bg-white border-t border-slate-100 shadow-2xl p-6 flex flex-col space-y-4 animate-in slide-in-from-top-2">
            <button onClick={() => { scrollToSection('plataforma'); setMobileMenuOpen(false); }} className="text-left py-3 font-semibold text-slate-700 border-b border-slate-50">A Plataforma</button>
            <button onClick={() => { navigate('/pricing'); setMobileMenuOpen(false); }} className="text-left py-3 font-semibold text-slate-700 border-b border-slate-50">Planos</button>
            <button 
              onClick={() => { scrollToSection('affiliates'); setMobileMenuOpen(false); }} 
              className="text-left py-3 font-bold text-emerald-700 border-b border-slate-50 flex items-center gap-2 outline-none"
            >
              <TrendingUp className="w-4 h-4" /> Programa de Parceiros
            </button>

            <div className="pt-4 flex flex-col gap-3">
              {user ? (
                 <Link to="/dashboard" className="bg-emerald-600 text-white py-4 rounded-xl text-center font-bold shadow-md flex items-center justify-center gap-2">
                   <LayoutDashboard className="w-5 h-5" /> Acessar Painel
                 </Link>
              ) : (
                <>
                  <Link to="/login" className="bg-slate-100 text-slate-900 py-4 rounded-xl text-center font-bold">Acessar Conta</Link>
                  <button 
                    onClick={() => { navigate('/pricing'); setMobileMenuOpen(false); }}
                    className="bg-emerald-600 text-white py-4 rounded-xl text-center font-bold shadow-md w-full"
                  >
                    Solicitar Demonstração
                  </button>
                </>
              )}
            </div>
          </div>
        )}
      </header>

      {/* =========================================================
          HERO SECTION (O Sistema Operacional - Mockup Interativo RETO)
          ========================================================= */}
      <section id="home" className="pt-32 pb-20 lg:pt-48 lg:pb-24 overflow-hidden relative bg-slate-50">
        {/* Background Decorativo Premium */}
        <div className="absolute top-0 right-0 -z-10 w-full h-full overflow-hidden pointer-events-none">
          <div className="absolute -top-[20%] -right-[10%] w-[70%] h-[70%] rounded-full bg-gradient-to-br from-emerald-100/40 to-emerald-50/10 blur-3xl"></div>
          <div className="absolute top-[40%] -left-[10%] w-[50%] h-[50%] rounded-full bg-gradient-to-tr from-blue-50/40 to-slate-100/10 blur-3xl"></div>
        </div>
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col lg:flex-row items-center gap-16">
            
            {/* Copy Principal */}
            <div className="lg:w-1/2 space-y-8 animate-in slide-in-from-left duration-700 relative z-10">
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white border border-slate-200 shadow-sm">
                <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
                <span className="text-xs font-bold text-slate-600 uppercase tracking-wide">Plataforma All-in-One</span>
              </div>
              
              <h1 className="text-4xl lg:text-[4rem] font-black text-slate-900 leading-[1.1] tracking-tight">
                Gestão condominial completa em um <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-600 to-emerald-400">único lugar.</span>
              </h1>
              
              <p className="text-lg lg:text-xl text-slate-600 leading-relaxed max-w-lg">
                Assembleias online, reservas de ambientes, controle de convidados via QR Code, abertura de chamados e comunicação centralizada. Tudo organizado, registrado e acessível em tempo real.
              </p>
              
              <div className="flex flex-col sm:flex-row gap-4 pt-4">
                {user ? (
                  <Link 
                    to="/dashboard"
                    className="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-4 rounded-full font-bold text-center shadow-xl shadow-emerald-500/20 transition-all transform hover:-translate-y-1 flex items-center justify-center gap-2"
                  >
                    <LayoutDashboard className="w-5 h-5" />
                    Ir para meu Dashboard
                  </Link>
                ) : (
                  <>
                    <button 
                      onClick={() => navigate('/pricing')}
                      className="bg-slate-900 hover:bg-slate-800 text-white px-8 py-4 rounded-full font-bold text-center shadow-xl transition-all transform hover:-translate-y-1"
                    >
                      Solicitar demonstração
                    </button>
                    <button 
                      onClick={() => scrollToSection('plataforma')}
                      className="bg-white border-2 border-slate-200 hover:border-slate-300 text-slate-700 hover:text-slate-900 px-8 py-4 rounded-full font-bold transition-all flex items-center justify-center gap-2 group"
                    >
                      Ver como funciona <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                    </button>
                  </>
                )}
              </div>
              
              <p className="text-sm font-medium text-slate-500 flex items-center gap-2 pt-2">
                <CheckCircle className="w-4 h-4 text-emerald-500" /> Teste grátis por 30 dias. Sem cartão de crédito.
              </p>
            </div>
            
            {/* Mockup UI REAL DA DASHBOARD - Interativo (ALINHAMENTO RETO) */}
            <div className="lg:w-1/2 relative w-full animate-in slide-in-from-right duration-1000 hidden md:block group cursor-pointer" title="Clique no menu para interagir com o sistema">
              <div className="relative bg-slate-50 rounded-2xl shadow-2xl shadow-slate-300/50 border border-slate-200 overflow-hidden aspect-[16/11] flex select-none transition-shadow duration-500 hover:shadow-[0_20px_50px_rgba(0,0,0,0.15)]">

                {/* Sidebar Dark */}
                <div className="w-[28%] bg-[#0f172a] flex flex-col justify-between py-4 border-r border-slate-800 relative z-10">
                  <div>
                    {/* Logo Area */}
                    <div className="flex items-center gap-2 px-3 mb-6" onClick={() => setActiveMockupTab('dashboard')}>
                      <div className="w-6 h-6 bg-emerald-500 rounded-md flex items-center justify-center">
                        <LayoutDashboard className="w-3 h-3 text-white" />
                      </div>
                      <span className="text-white font-bold text-sm tracking-wide">Votzz<span className="text-emerald-500">.</span></span>
                    </div>

                    {/* Menu Items Dinâmicos Baseados no Print */}
                    <div className="space-y-1 mt-6 px-2">
                      {[
                        { id: 'dashboard', icon: LayoutDashboard, label: "Dashboard" },
                        { id: 'governanca', icon: ShieldCheck, label: "Governança" },
                        { id: 'assembleias', icon: Users, label: "Assembleias" },
                        { id: 'encomendas', icon: Package, label: "Encomendas" },
                        { id: 'convidados', icon: UserCheck, label: "Convidados" }, // NOVO NO MENU HERO
                        { id: 'reservas', icon: CalendarRange, label: "Espaços & Reservas" },
                        { id: 'chamados', icon: MessageSquare, label: "Chamados & Ajuda" },
                        { id: 'relatorios', icon: FileText, label: "Relatórios" }
                      ].map((menu) => (
                        <div 
                          key={menu.id} 
                          onClick={() => setActiveMockupTab(menu.id)}
                          className={`flex items-center gap-2 px-3 py-2 rounded-lg transition-colors duration-300 ${
                            activeMockupTab === menu.id 
                            ? 'bg-emerald-500 text-white shadow-sm' 
                            : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                          }`}
                        >
                          <menu.icon className="w-3.5 h-3.5" />
                          <span className="text-[10px] font-medium">{menu.label}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* User Profile Area Corrigida */}
                  <div className="flex items-center gap-2 px-3 pt-4 border-t border-slate-800 mx-2">
                    <div className="w-7 h-7 rounded-full bg-slate-800 border border-slate-600 text-emerald-400 flex items-center justify-center text-[10px] font-black">
                      S
                    </div>
                    <div>
                      <p className="text-white text-[10px] font-bold leading-tight">Síndico</p>
                      <p className="text-emerald-500 text-[8px] leading-tight font-medium">Conta Profissional</p>
                    </div>
                  </div>
                </div>

                {/* Main Content Area (Renderiza baseado no state) */}
                <div className="w-[72%] bg-white flex flex-col relative z-0">
                  
                  {/* Top Header */}
                  <div className="px-5 py-4 flex justify-between items-end border-b border-slate-100 bg-white z-10">
                    <div>
                      <h3 className="text-base font-black text-slate-800 leading-none mb-1.5">Olá, Síndico</h3>
                      <div className="flex items-center gap-1.5 text-[9px] text-slate-500 font-medium">
                        <Building className="w-3 h-3 text-emerald-500" /> Condomínio Votzz
                      </div>
                    </div>
                    <div className="bg-emerald-50 border border-emerald-100 text-emerald-700 px-2.5 py-1 rounded-full text-[8px] font-bold uppercase tracking-wider">
                      Votzz OS 2.0
                    </div>
                  </div>

                  {/* CONTEÚDO DINÂMICO DO DASHBOARD */}
                  <div className="p-5 flex-1 flex flex-col gap-3 bg-slate-50/50 overflow-hidden relative">
                    
                    {/* VISÃO: DASHBOARD PRINCIPAL */}
                    {activeMockupTab === 'dashboard' && (
                      <div className="animate-in fade-in duration-300 h-full flex flex-col gap-3">
                        {/* Row 1: Key Metrics */}
                        <div className="flex gap-3 h-[85px]">
                          {/* Dark Balance Card */}
                          <div className="w-[45%] bg-[#0a0f1c] rounded-xl p-3.5 relative overflow-hidden flex flex-col justify-between shadow-md">
                            <div className="absolute -right-2 -bottom-2 opacity-[0.03]">
                              <DollarSign className="w-20 h-20 text-white" />
                            </div>
                            <div>
                              <p className="text-[8px] text-emerald-500 font-black uppercase tracking-widest mb-1">Saldo Atual em Caixa</p>
                              <p className="text-2xl font-black text-white tracking-tight">R$ 1.014,00</p>
                            </div>
                            <div className="w-fit bg-emerald-600 text-white text-[7px] font-bold px-2 py-1 rounded border border-emerald-500 mt-1">
                               Atualizar Manualmente
                            </div>
                          </div>

                          {/* Small KPI Cards */}
                          <div className="w-[55%] flex gap-3">
                            <div className="flex-1 bg-white border border-slate-200 rounded-xl p-3 flex flex-col justify-between shadow-sm">
                              <Users className="w-4 h-4 text-blue-500" />
                              <div>
                                <p className="text-lg font-black text-slate-800 leading-none">142</p>
                                <p className="text-[7px] text-slate-500 font-bold mt-1 uppercase tracking-wider">Moradores</p>
                              </div>
                            </div>
                            <div className="flex-1 bg-white border border-slate-200 rounded-xl p-3 flex flex-col justify-between shadow-sm cursor-pointer" onClick={() => setActiveMockupTab('convidados')}>
                              <UserCheck className="w-4 h-4 text-emerald-500" />
                              <div>
                                <p className="text-lg font-black text-slate-800 leading-none">5</p>
                                <p className="text-[7px] text-slate-500 font-bold mt-1 uppercase tracking-wider leading-tight">Visitas<br/>Hoje</p>
                              </div>
                            </div>
                          </div>
                        </div>

                        {/* Row 2: Action Buttons */}
                        <div className="grid grid-cols-3 gap-3 h-11">
                          <div onClick={() => setActiveMockupTab('assembleias')} className="bg-emerald-600 rounded-xl flex flex-col items-center justify-center text-white shadow-sm hover:bg-emerald-500 transition-colors cursor-pointer">
                            <Video className="w-3.5 h-3.5 mb-0.5" /> <span className="text-[8px] font-bold">Assembleias</span>
                          </div>
                          <div onClick={() => setActiveMockupTab('convidados')} className="bg-blue-600 rounded-xl flex flex-col items-center justify-center text-white shadow-sm hover:bg-blue-500 transition-colors cursor-pointer">
                            <QrCode className="w-3.5 h-3.5 mb-0.5" /> <span className="text-[8px] font-bold">Convidados</span>
                          </div>
                          <div onClick={() => setActiveMockupTab('chamados')} className="bg-purple-600 rounded-xl flex flex-col items-center justify-center text-white shadow-sm hover:bg-purple-500 transition-colors cursor-pointer">
                            <Wrench className="w-3.5 h-3.5 mb-0.5" /> <span className="text-[8px] font-bold">Chamados</span>
                          </div>
                        </div>

                        {/* Chart Widget */}
                        <div className="w-full bg-white border border-slate-200 rounded-xl p-4 shadow-sm flex flex-col relative overflow-hidden flex-1">
                          <h4 className="text-[10px] font-black text-slate-800 mb-1">Engajamento nas Votações</h4>
                          <div className="absolute bottom-0 left-0 right-0 h-[70%] flex items-end">
                            <svg viewBox="0 0 100 40" className="w-full h-full" preserveAspectRatio="none">
                              <path d="M0,40 L10,35 L20,5 L30,30 L40,38 L100,40 Z" fill="url(#grad)" opacity="0.4"></path>
                              <path d="M0,40 L10,35 L20,5 L30,30 L40,38 L100,40" fill="none" stroke="#10b981" strokeWidth="1.5" vectorEffect="non-scaling-stroke"></path>
                              <defs>
                                <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                                  <stop offset="0%" stopColor="#10b981" />
                                  <stop offset="100%" stopColor="rgba(16,185,129,0)" />
                                </linearGradient>
                              </defs>
                            </svg>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* VISÃO: CONVIDADOS (NOVO MODULO NO MOCKUP) */}
                    {activeMockupTab === 'convidados' && (
                      <div className="animate-in fade-in duration-300 h-full flex flex-col">
                        <div className="flex justify-between items-center mb-3">
                            <h4 className="text-xs font-black text-slate-800">Convidados e Acessos</h4>
                            <button className="bg-emerald-600 text-white px-2 py-1 rounded-lg text-[8px] font-bold shadow-sm flex items-center gap-1"><Plus className="w-3 h-3"/> Novo Convite</button>
                        </div>
                        <div className="flex gap-3 h-full pb-2">
                            {/* Card de Convidado Estilo Imagem 2 */}
                            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col relative w-1/2 h-full">
                                <div className="absolute top-3 right-3 flex gap-1">
                                  <QrCode className="w-3 h-3 text-slate-400"/>
                                </div>
                                <h5 className="font-bold text-slate-800 text-sm mb-0.5">Carlos Visitante</h5>
                                <p className="text-[8px] text-slate-500 font-medium mb-3">RG: 11.222.333-4</p>
                                
                                <div className="text-[8px] text-slate-600 mb-3">
                                    Agendado: Hoje às 15:00
                                </div>
                                
                                <div className="text-[8px] text-slate-500 mb-3 flex-1">
                                    <p className="font-bold text-slate-400 uppercase text-[7px] mb-0.5">Autorizado por</p>
                                    <p className="font-bold text-slate-800 text-[10px] mb-1">João Síndico</p>
                                    <p>Bl A | Ap 101</p>
                                </div>

                                <button className="mt-auto w-full bg-slate-900 text-white py-1.5 rounded-lg text-[8px] font-bold hover:text-emerald-400 transition-colors">Autorizar Manualmente</button>
                            </div>
                            
                            {/* Simulação do Modal de QRCode */}
                            <div className="bg-white border-2 border-slate-900 p-3 rounded-xl shadow-sm flex flex-col items-center justify-center relative w-1/2 h-full text-center">
                                <h5 className="font-black text-slate-900 text-[10px] mb-1 uppercase tracking-tighter">Passe de Acesso</h5>
                                <p className="text-[8px] text-slate-500 font-bold mb-2">Jota</p>
                                <div className="bg-white p-1 border-2 border-slate-900 rounded-lg mb-2">
                                    <QrCode className="w-10 h-10 text-slate-900" />
                                </div>
                                <p className="text-[7px] text-slate-400 mb-3">Apresente na portaria.</p>
                                <button className="w-full bg-emerald-500 text-white py-1.5 rounded-lg text-[8px] font-bold flex items-center justify-center gap-1"><Smartphone className="w-2.5 h-2.5"/> Enviar WhatsApp</button>
                            </div>
                        </div>
                      </div>
                    )}

                    {/* VISÃO: GOVERNANÇA E RELATÓRIOS (Placeholder para as novas abas) */}
                    {(activeMockupTab === 'governanca' || activeMockupTab === 'relatorios') && (
                      <div className="animate-in fade-in duration-300 h-full flex flex-col items-center justify-center text-slate-400">
                        <ShieldCheck className="w-8 h-8 mb-2 opacity-50" />
                        <p className="text-[10px] font-bold">Área Restrita</p>
                      </div>
                    )}

                    {/* VISÃO: ASSEMBLEIAS */}
                    {activeMockupTab === 'assembleias' && (
                      <div className="animate-in fade-in duration-300 h-full flex flex-col">
                        <div className="flex justify-between items-center mb-4">
                            <h4 className="text-sm font-black text-slate-800">Assembleias e Votações online</h4>
                            <button className="bg-emerald-600 text-white px-3 py-1.5 rounded-lg text-[9px] font-bold shadow-sm flex items-center gap-1"><Plus className="w-3 h-3"/> Nova Assembleia</button>
                        </div>
                        <div className="bg-slate-900 flex-1 rounded-xl shadow-lg border border-slate-800 relative overflow-hidden p-4 flex flex-col justify-center">
                            <div className="absolute top-3 right-3 flex items-center gap-1.5 bg-slate-800/80 px-2 py-1 rounded-md">
                                <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse"></div>
                                <span className="text-[8px] font-bold text-red-400 uppercase tracking-widest">Ao Vivo</span>
                            </div>
                            <div className="w-12 h-12 bg-emerald-500/20 rounded-full flex items-center justify-center mx-auto mb-3">
                               <Video className="w-5 h-5 text-emerald-400" />
                            </div>
                            <h5 className="text-white text-center text-[12px] font-black mb-1">Assembleia Extraordinária</h5>
                            <p className="text-slate-400 text-center text-[9px] mb-4">Pauta 1: Aprovação de Contas</p>
                            <button className="w-3/4 mx-auto bg-emerald-600 text-white py-2 rounded-lg text-[10px] font-bold hover:bg-emerald-500 transition-colors">Entrar na Sala Virtual</button>
                        </div>
                      </div>
                    )}

                    {/* VISÃO: ENCOMENDAS */}
                    {activeMockupTab === 'encomendas' && (
                      <div className="animate-in fade-in duration-300 h-full flex flex-col">
                        <div className="flex justify-between items-center mb-4">
                            <h4 className="text-sm font-black text-slate-800">Gestão de Encomendas</h4>
                            <button className="bg-emerald-600 text-white px-3 py-1.5 rounded-lg text-[9px] font-bold shadow-sm flex items-center gap-1"><Plus className="w-3 h-3"/> Registrar Entrega</button>
                        </div>
                        <div className="space-y-2 overflow-y-auto pr-1">
                            <div className="bg-white p-3 rounded-xl border border-slate-200 flex justify-between items-center shadow-sm">
                                <div className="flex items-center gap-3">
                                   <div className="w-8 h-8 bg-amber-50 rounded-lg flex justify-center items-center border border-amber-100"><Package className="w-4 h-4 text-amber-600" /></div>
                                   <div><p className="text-[10px] font-bold text-slate-800 leading-tight">Apt 402 - Bloco B</p><p className="text-[8px] text-slate-500">Mercado Livre</p></div>
                                </div>
                                <span className="bg-amber-100 text-amber-700 px-2 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider">Aguardando</span>
                            </div>
                            <div className="bg-white p-3 rounded-xl border border-slate-200 flex justify-between items-center shadow-sm">
                                <div className="flex items-center gap-3">
                                   <div className="w-8 h-8 bg-blue-50 rounded-lg flex justify-center items-center border border-blue-100"><Package className="w-4 h-4 text-blue-600" /></div>
                                   <div><p className="text-[10px] font-bold text-slate-800 leading-tight">Apt 105 - Bloco A</p><p className="text-[8px] text-slate-500">Amazon</p></div>
                                </div>
                                <span className="bg-amber-100 text-amber-700 px-2 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider">Aguardando</span>
                            </div>
                            <div className="bg-white p-3 rounded-xl border border-slate-200 flex justify-between items-center opacity-60">
                                <div className="flex items-center gap-3">
                                   <div className="w-8 h-8 bg-slate-100 rounded-lg flex justify-center items-center"><Package className="w-4 h-4 text-slate-400" /></div>
                                   <div><p className="text-[10px] font-bold text-slate-800 leading-tight">Apt 301 - Bloco C</p><p className="text-[8px] text-slate-500">Sedex</p></div>
                                </div>
                                <span className="bg-slate-100 text-slate-500 px-2 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider">Retirado</span>
                            </div>
                        </div>
                      </div>
                    )}

                    {/* VISÃO: RESERVAS */}
                    {activeMockupTab === 'reservas' && (
                      <div className="animate-in fade-in duration-300 h-full flex flex-col">
                        <div className="flex justify-between items-center mb-4">
                            <h4 className="text-sm font-black text-slate-800">Votzz Spaces</h4>
                            <span className="text-[9px] font-bold text-slate-500 underline cursor-pointer">Ver Minha Agenda</span>
                        </div>
                        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col gap-2">
                             <div className="w-full h-24 bg-emerald-50 rounded-lg flex items-center justify-center border border-emerald-100 relative overflow-hidden">
                                 <CalendarRange className="w-10 h-10 text-emerald-200 absolute right-4 opacity-50 transform scale-150"/>
                                 <h5 className="text-emerald-800 font-black text-lg z-10 relative">Churrasqueira VIP</h5>
                             </div>
                             <div className="flex justify-between items-end mt-2">
                                 <div>
                                    <p className="text-[9px] text-slate-500 font-bold mb-0.5">Taxa de uso</p>
                                    <h5 className="text-[13px] font-black text-slate-800">R$ 130,00</h5>
                                 </div>
                                 <button className="bg-slate-900 text-white px-5 py-2 rounded-lg text-[10px] font-bold shadow-md hover:bg-emerald-600 transition-colors">Agendar Uso</button>
                             </div>
                        </div>
                      </div>
                    )}

                    {/* VISÃO: CHAMADOS */}
                    {activeMockupTab === 'chamados' && (
                      <div className="animate-in fade-in duration-300 h-full flex flex-col">
                        <div className="flex justify-between items-center mb-4">
                            <h4 className="text-sm font-black text-slate-800">Central de Chamados</h4>
                            <button className="bg-blue-600 text-white px-3 py-1.5 rounded-lg text-[9px] font-bold shadow-sm flex items-center gap-1"><Plus className="w-3 h-3"/> Novo Chamado</button>
                        </div>
                        <div className="space-y-2">
                            <div className="bg-white p-3 rounded-xl border border-slate-200 flex justify-between items-center shadow-sm border-l-4 border-l-red-500">
                                <div className="flex items-center gap-3">
                                   <div className="w-8 h-8 bg-red-50 rounded-lg flex justify-center items-center"><Wrench className="w-4 h-4 text-red-500" /></div>
                                   <div><p className="text-[10px] font-bold text-slate-800 leading-tight">Vazamento Garagem G2</p><p className="text-[8px] text-slate-500 mt-0.5">Aberto por: Apt 102</p></div>
                                </div>
                                <span className="bg-red-100 text-red-700 px-2 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider">Urgente</span>
                            </div>
                            <div className="bg-white p-3 rounded-xl border border-slate-200 flex justify-between items-center shadow-sm border-l-4 border-l-amber-500">
                                <div className="flex items-center gap-3">
                                   <div className="w-8 h-8 bg-amber-50 rounded-lg flex justify-center items-center"><MessageSquare className="w-4 h-4 text-amber-500" /></div>
                                   <div><p className="text-[10px] font-bold text-slate-800 leading-tight">Barulho após 22h</p><p className="text-[8px] text-slate-500 mt-0.5">Aberto por: Apt 501</p></div>
                                </div>
                                <span className="bg-amber-100 text-amber-700 px-2 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider">Análise</span>
                            </div>
                        </div>
                      </div>
                    )}

                  </div>
                </div>

                {/* Float Element Decorativo - Dinâmico para chamar atenção ao QRCode */}
                <div className="absolute bottom-6 right-6 bg-white p-3 rounded-xl shadow-[0_10px_30px_rgba(0,0,0,0.15)] border border-slate-200 flex items-center gap-3 animate-bounce hover:animate-none transition-all cursor-pointer z-30" onClick={() => setActiveMockupTab('convidados')}>
                  <div className="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center border border-emerald-200">
                    <QrCode className="w-5 h-5 text-emerald-600" />
                  </div>
                  <div className="pr-2">
                    <p className="text-[8px] font-black text-slate-500 uppercase tracking-widest leading-tight">Novo Acesso</p>
                    <p className="text-xs font-bold text-slate-900 mt-0.5 leading-tight">Visitante VIP Autorizado</p>
                  </div>
                </div>
              </div>
            </div>
            
            {/* Fallback image if mobile (keeps UI clean) */}
            <div className="lg:hidden w-full px-4">
              <div className="bg-slate-900 rounded-2xl aspect-video flex flex-col items-center justify-center shadow-2xl border border-slate-800 p-6 text-center">
                 <LayoutDashboard className="w-12 h-12 text-emerald-500 mb-4" />
                 <p className="text-white font-bold text-lg mb-2">Votzz OS 2.0</p>
                 <p className="text-slate-400 text-sm">Acesse pelo computador para ver a demonstração interativa do sistema.</p>
              </div>
            </div>

          </div>
        </div>

        {/* Social Proof Strip */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-24 border-t border-slate-200/60 pt-10">
          <p className="text-center text-sm font-bold text-slate-400 uppercase tracking-widest mb-6">Confiado por +500 condomínios e administradoras</p>
          <div className="flex justify-center gap-8 md:gap-16 opacity-50 flex-wrap">
            <div className="flex items-center gap-2 grayscale hover:grayscale-0 transition-all"><Building className="w-6 h-6"/> <span className="text-lg font-black tracking-tighter">BILDING</span></div>
            <div className="flex items-center gap-2 grayscale hover:grayscale-0 transition-all"><ShieldCheck className="w-6 h-6"/> <span className="text-lg font-black tracking-tighter">SECURA</span></div>
            <div className="flex items-center gap-2 grayscale hover:grayscale-0 transition-all"><Crown className="w-6 h-6"/> <span className="text-lg font-black tracking-tighter">PRIMEGEST</span></div>
            <div className="flex items-center gap-2 grayscale hover:grayscale-0 transition-all hidden md:flex"><CheckCircle className="w-6 h-6"/> <span className="text-lg font-black tracking-tighter">EXATA</span></div>
            <div className="flex items-center gap-2 grayscale hover:grayscale-0 transition-all hidden md:flex"><BarChart3 className="w-6 h-6"/> <span className="text-lg font-black tracking-tighter">METRIX</span></div>
          </div>
        </div>
      </section>

      {/* =========================================================
          3 PILARES DA PLATAFORMA (O CORAÇÃO DA ESTRATÉGIA ALINHADO RETO)
          ========================================================= */}
      <section id="plataforma" className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          
          <div className="text-center max-w-3xl mx-auto mb-20">
            <span className="text-emerald-600 font-bold tracking-widest uppercase text-sm block mb-3">A Infraestrutura Digital</span>
            <h2 className="text-3xl lg:text-5xl font-black text-slate-900 mb-6 tracking-tight">O sistema operacional do seu condomínio.</h2>
            <p className="text-lg text-slate-600 leading-relaxed">
              Substitua os grupos confusos de WhatsApp, planilhas perdidas e o caderno da portaria por uma única plataforma centralizada.
            </p>
          </div>

          <div className="space-y-32">
            
            {/* PILAR 1: COMUNICAÇÃO */}
            <div className="flex flex-col lg:flex-row items-center gap-16">
              <div className="lg:w-1/2 space-y-6">
                <div className="w-14 h-14 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center mb-6">
                  <Megaphone className="w-7 h-7" />
                </div>
                <h3 className="text-3xl font-black text-slate-900">Comunicação</h3>
                <div className="bg-slate-100/50 border-l-4 border-slate-900 p-4 rounded-r-xl">
                  <p className="text-xl font-bold text-slate-900">Menos improviso. Mais organização.</p>
                </div>
                <p className="text-slate-600 leading-relaxed text-lg">
                  Mantenha todos os moradores informados sem o ruído dos grupos de mensagens. Centralize as regras e avisos importantes onde todos podem ver.
                </p>
                <ul className="space-y-4 pt-4">
                  <li className="flex items-start gap-3">
                    <CheckCircle className="w-6 h-6 text-emerald-500 shrink-0" />
                    <div>
                      <strong className="block text-slate-900">Comunicados Oficiais</strong>
                      <span className="text-slate-500 text-sm">Dispare avisos importantes via aplicativo e e-mail com confirmação de leitura.</span>
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <CheckCircle className="w-6 h-6 text-emerald-500 shrink-0" />
                    <div>
                      <strong className="block text-slate-900">Histórico Acessível</strong>
                      <span className="text-slate-500 text-sm">Regimento interno, convenção e documentos do condomínio disponíveis 24/7.</span>
                    </div>
                  </li>
                </ul>
              </div>
              
              <div className="lg:w-1/2 w-full flex justify-center">
                {/* Mockup Mobile App RETO */}
                <div className="w-[300px] h-[600px] bg-slate-900 rounded-[2.5rem] p-3 shadow-2xl relative border-8 border-slate-800 hover:-translate-y-2 transition-all duration-500">
                  <div className="w-full h-full bg-white rounded-[2rem] overflow-hidden flex flex-col relative">
                    <div className="absolute top-0 inset-x-0 h-6 flex justify-center z-20">
                      <div className="w-32 h-5 bg-slate-900 rounded-b-2xl"></div>
                    </div>
                    <div className="bg-emerald-600 text-white pt-10 pb-4 px-6 rounded-b-3xl shadow-md z-10">
                      <p className="text-xs font-medium text-emerald-100">Residencial Aurora</p>
                      <h4 className="text-xl font-bold">Mural de Avisos</h4>
                    </div>
                    <div className="flex-1 bg-slate-50 p-4 space-y-4 overflow-hidden">
                      <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100">
                        <div className="flex items-center gap-3 mb-3">
                          <div className="w-10 h-10 bg-emerald-100 rounded-full flex items-center justify-center">
                            <Crown className="w-5 h-5 text-emerald-600" />
                          </div>
                          <div>
                            <p className="text-sm font-bold text-slate-900">Síndico</p>
                            <p className="text-[10px] text-slate-400">Hoje, 10:30</p>
                          </div>
                        </div>
                        <p className="text-xs text-slate-700 font-bold mb-1">Manutenção dos Elevadores</p>
                        <p className="text-xs text-slate-500 leading-relaxed mb-3">Aviso: O elevador social passará por manutenção preventiva amanhã das 14h às 16h.</p>
                        <div className="bg-slate-50 p-2 rounded-lg flex justify-between items-center border border-slate-100">
                          <span className="text-[10px] font-bold text-slate-400">Visto por 45 moradores</span>
                          <Check className="w-4 h-4 text-emerald-500" />
                        </div>
                      </div>
                      <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 opacity-70">
                        <div className="flex items-center gap-3 mb-3">
                          <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center">
                            <FileSignature className="w-5 h-5 text-slate-500" />
                          </div>
                          <div>
                            <p className="text-sm font-bold text-slate-900">Administração</p>
                            <p className="text-[10px] text-slate-400">Ontem, 16:00</p>
                          </div>
                        </div>
                        <p className="text-xs text-slate-700 font-bold mb-1">Novo Regimento Interno</p>
                        <div className="h-10 w-full bg-slate-100 rounded flex items-center justify-center mt-2">
                          <span className="text-xs font-bold text-emerald-600">Baixar PDF</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* PILAR 2: OPERAÇÃO (Layout Invertido, COM MÓDULO CONVIDADOS ATUALIZADO) */}
            <div className="flex flex-col-reverse lg:flex-row items-center gap-16">
              <div className="lg:w-1/2 w-full">
                {/* Mockup Desktop Dashboard RETO */}
                <div className="bg-white rounded-2xl shadow-xl shadow-slate-200/60 border border-slate-200 overflow-hidden hover:-translate-y-2 transition-all duration-500">
                  <div className="bg-slate-50 border-b border-slate-200 p-4 flex gap-2">
                    <div className="w-3 h-3 rounded-full bg-slate-300"></div>
                    <div className="w-3 h-3 rounded-full bg-slate-300"></div>
                    <div className="w-3 h-3 rounded-full bg-slate-300"></div>
                  </div>
                  <div className="p-6 bg-slate-50">
                    <div className="grid gap-6">
                      
                      {/* Encomendas */}
                      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                        <div className="flex items-center gap-4">
                          <div className="w-12 h-12 bg-amber-50 rounded-lg flex items-center justify-center">
                            <Package className="w-6 h-6 text-amber-500" />
                          </div>
                          <div>
                            <h5 className="font-bold text-slate-900 text-sm">Controle de Encomendas</h5>
                            <p className="text-xs text-slate-500">Aguardando retirada: 12 pacotes</p>
                          </div>
                        </div>
                        <button className="bg-slate-900 text-white px-4 py-2 rounded-lg text-xs font-bold">Registrar Novo</button>
                      </div>
                      
                      {/* NOVO: CONVIDADOS NO MOCKUP ESTATICO DA OPERACAO */}
                      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between relative overflow-hidden">
                        <div className="absolute left-0 top-0 bottom-0 w-1 bg-blue-500"></div>
                        <div className="flex items-center gap-4 pl-2">
                          <div className="w-12 h-12 bg-blue-50 rounded-lg flex items-center justify-center">
                            <QrCode className="w-6 h-6 text-blue-600" />
                          </div>
                          <div>
                            <h5 className="font-bold text-slate-900 text-sm">Acesso de Convidados</h5>
                            <p className="text-xs text-slate-500">Carlos Visitante • Passe QR Code Gerado</p>
                          </div>
                        </div>
                        <span className="bg-blue-100 text-blue-700 px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider">Pendente</span>
                      </div>

                      {/* Reservas */}
                      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between relative overflow-hidden">
                        <div className="absolute left-0 top-0 bottom-0 w-1 bg-emerald-500"></div>
                        <div className="flex items-center gap-4 pl-2">
                          <div className="w-12 h-12 bg-emerald-50 rounded-lg flex items-center justify-center">
                            <CalendarRange className="w-6 h-6 text-emerald-600" />
                          </div>
                          <div>
                            <h5 className="font-bold text-slate-900 text-sm">Reservas (Salão de Festas)</h5>
                            <p className="text-xs text-slate-500">Apt 301 • Sábado, 19:00 - 23:00</p>
                          </div>
                        </div>
                        <span className="bg-emerald-100 text-emerald-700 px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-wider">Confirmado</span>
                      </div>

                    </div>
                  </div>
                </div>
              </div>

              <div className="lg:w-1/2 space-y-6">
                <div className="w-14 h-14 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center mb-6">
                  <Building className="w-7 h-7" />
                </div>
                <h3 className="text-3xl font-black text-slate-900">Operação</h3>
                <div className="bg-emerald-50 border-l-4 border-emerald-500 p-4 rounded-r-xl">
                  <p className="text-xl font-bold text-emerald-800">Todas as demandas organizadas em um único lugar.</p>
                </div>
                <p className="text-slate-600 leading-relaxed text-lg">
                  Livre-se da papelada na portaria e das planilhas de agendamento. Digitalize a rotina e dê autonomia para o morador e controle absoluto para a gestão.
                </p>
                <ul className="space-y-4 pt-4">
                  {/* NOVO ITEM: CONVIDADOS */}
                  <li className="flex items-start gap-3">
                    <UserCheck className="w-6 h-6 text-emerald-500 shrink-0" />
                    <div>
                      <strong className="block text-slate-900">Controle de Convidados via QR Code</strong>
                      <span className="text-slate-500 text-sm">O morador gera um Passe de Acesso seguro e envia a imagem direto pelo WhatsApp. A portaria apenas escaneia a tela do celular.</span>
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <Package className="w-6 h-6 text-emerald-500 shrink-0" />
                    <div>
                      <strong className="block text-slate-900">Controle de Encomenda</strong>
                      <span className="text-slate-500 text-sm">Registro rápido na portaria com notificação automática push no celular do morador.</span>
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <CalendarRange className="w-6 h-6 text-emerald-500 shrink-0" />
                    <div>
                      <strong className="block text-slate-900">Reservas de Ambientes</strong>
                      <span className="text-slate-500 text-sm">Salão de festas, churrasqueira e agendamento de mudanças com regras automatizadas.</span>
                    </div>
                  </li>
                </ul>
              </div>
            </div>

            {/* PILAR 3: GOVERNANÇA E DECISÃO */}
            <div className="flex flex-col lg:flex-row items-center gap-16">
              <div className="lg:w-1/2 space-y-6">
                <div className="w-14 h-14 bg-amber-50 text-amber-600 rounded-2xl flex items-center justify-center mb-6">
                  <Scale className="w-7 h-7" />
                </div>
                <h3 className="text-3xl font-black text-slate-900">Decisão e Governança</h3>
                <div className="bg-slate-900 border-l-4 border-emerald-500 p-4 rounded-r-xl">
                  <p className="text-xl font-bold text-white">Mais participação. Mais transparência. Mais segurança jurídica.</p>
                </div>
                <p className="text-slate-600 leading-relaxed text-lg">
                  O módulo que deu origem à Votzz. Transforme assembleias desgastantes em processos digitais eficientes, auditáveis e com alta taxa de adesão.
                </p>
                <ul className="space-y-4 pt-4">
                  <li className="flex items-start gap-3">
                    <Video className="w-6 h-6 text-emerald-500 shrink-0" />
                    <div>
                      <strong className="block text-slate-900">Assembleias Online</strong>
                      <span className="text-slate-500 text-sm">Transmissão de vídeo nativa na plataforma, controle automático de quórum e lista de presença digital.</span>
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <Lock className="w-6 h-6 text-emerald-500 shrink-0" />
                    <div>
                      <strong className="block text-slate-900">Votação Segura</strong>
                      <span className="text-slate-500 text-sm">Registros imutáveis, controle de pesos (frações ideais) e geração de ata automática em tempo real.</span>
                    </div>
                  </li>
                </ul>
                <div className="pt-2">
                  <button onClick={() => navigate('/pricing')} className="text-emerald-600 font-bold hover:text-emerald-700 flex items-center gap-2 group">
                    Conhecer Módulo de Assembleias <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </button>
                </div>
              </div>
              
              <div className="lg:w-1/2 w-full">
                 {/* Mockup Desktop Assembleia RETO */}
                 <div className="bg-slate-900 rounded-3xl p-3 shadow-2xl border border-slate-700/50 hover:-translate-y-2 transition-all duration-500">
                    <div className="bg-slate-800 rounded-2xl overflow-hidden aspect-[4/3] flex flex-col">
                      <div className="bg-slate-900 p-3 flex justify-between items-center border-b border-slate-700">
                        <div className="flex items-center gap-2">
                          <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse"></div>
                          <span className="text-[10px] text-red-400 font-black uppercase tracking-wider">Ao Vivo</span>
                        </div>
                        <span className="text-[10px] text-slate-400 font-bold">142 Presentes</span>
                      </div>
                      <div className="flex-1 bg-slate-800 flex flex-col p-6 items-center justify-center text-center relative overflow-hidden">
                        <div className="absolute inset-0 opacity-10 flex items-center justify-center">
                          <Users className="w-40 h-40 text-white" />
                        </div>
                        
                        <div className="relative z-10 w-full max-w-sm bg-slate-900/90 backdrop-blur-md p-6 rounded-2xl border border-slate-700 shadow-xl">
                          <p className="text-xs text-emerald-400 font-bold uppercase tracking-widest mb-2">Votação Aberta</p>
                          <h4 className="text-lg font-black text-white mb-6 leading-tight">Aprovação da Previsão Orçamentária 2026</h4>
                          
                          <div className="space-y-4">
                            <div>
                              <div className="flex justify-between text-xs text-slate-300 mb-1 font-medium">
                                <span>Aprovado</span>
                                <span>78%</span>
                              </div>
                              <div className="h-3 w-full bg-slate-800 rounded-full overflow-hidden">
                                <div className="h-full bg-emerald-500 w-[78%]"></div>
                              </div>
                            </div>
                            
                            <div>
                              <div className="flex justify-between text-xs text-slate-300 mb-1 font-medium">
                                <span>Não Aprovado</span>
                                <span>22%</span>
                              </div>
                              <div className="h-3 w-full bg-slate-800 rounded-full overflow-hidden">
                                <div className="h-full bg-red-500 w-[22%]"></div>
                              </div>
                            </div>
                          </div>
                          
                          <div className="mt-6 pt-4 border-t border-slate-700 flex justify-center gap-2">
                             <button className="bg-slate-800 text-slate-400 px-4 py-2 rounded-lg text-xs font-bold hover:bg-slate-700">Aguardando Encerramento</button>
                          </div>
                        </div>
                      </div>
                    </div>
                 </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* =========================================================
          COMO FUNCIONA
          ========================================================= */}
      <section id="como-funciona" className="py-24 bg-emerald-50/50 border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-20">
             <h2 className="text-3xl lg:text-5xl font-black text-slate-900 mb-4 tracking-tight">Implantação sem dor de cabeça.</h2>
             <p className="text-slate-600 max-w-xl mx-auto text-lg">O setup mais rápido do mercado. Rodando e integrado no seu condomínio em poucas etapas.</p>
          </div>

          <div className="grid md:grid-cols-5 gap-6 relative">
            {[
              { num: "01", title: "Crie a conta", text: "Cadastre os dados base do condomínio em menos de 1 minuto." },
              { num: "02", title: "Configure", text: "Configure os módulos." },
              { num: "03", title: "Inicie", text: "Comece a rodar a gestão da sua equipe no Votzz OS." },
              { num: "04", title: "Gerencie", text: "Acompanhe tudo em tempo real pelo seu painel central." },
              { num: "05", title: "Decida", text: "Realize assembleias e votações online seguras com validade jurídica." },
            ].map((step, idx) => (
              <div key={idx} className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 relative z-10 text-center hover:shadow-xl hover:-translate-y-2 hover:border-emerald-200 transition-all duration-300 group">
                  <div className="w-12 h-12 bg-slate-100 group-hover:bg-emerald-600 group-hover:text-white text-slate-400 font-black rounded-full flex items-center justify-center mx-auto mb-6 text-lg transition-colors">
                    {step.num}
                  </div>
                  <h4 className="font-bold text-slate-900 mb-2 leading-tight">{step.title}</h4>
                  <p className="text-xs text-slate-500 leading-relaxed">{step.text}</p>
              </div>
            ))}
            <div className="hidden md:block absolute top-[44px] left-10 right-10 h-0.5 bg-slate-200 -z-0"></div>
          </div>
        </div>
      </section>

      {/* =========================================================
          PRICING
          ========================================================= */}
      <section id="pricing" className="py-32 bg-[#0a0f1c] text-white relative overflow-hidden">
        {/* Glow Effects */}
        <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-emerald-600/10 rounded-full blur-[100px]"></div>
        <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-blue-600/10 rounded-full blur-[100px]"></div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="text-center mb-20">
            <div className="inline-flex items-center gap-2 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-bold px-4 py-1.5 rounded-full mb-6 text-sm uppercase tracking-wider">
              <Gift className="w-4 h-4" /> Plano Free: 30 dias grátis
            </div>
            <h2 className="text-4xl lg:text-5xl font-black mb-6 tracking-tight">Comece grátis. Renove se resolver.</h2>
            <p className="text-slate-400 max-w-2xl mx-auto text-lg">
              Todos os condomínios começam com o <strong>Plano Free</strong>. 
              Implante, teste a adesão dos moradores e a potência do Votzz OS sem compromisso.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8 max-w-6xl mx-auto items-stretch">
            {/* PLANO 1 */}
            <div className="bg-slate-800/40 backdrop-blur-md rounded-3xl p-8 border border-slate-700/50 hover:border-emerald-500/50 hover:bg-slate-800/60 transition-all flex flex-col relative overflow-hidden group">
              <h3 className="text-2xl font-black text-slate-200">Essencial</h3>
              <p className="text-xs text-emerald-400 font-bold uppercase tracking-wider mb-4 mt-1">Até 30 Unidades</p>
              <div className="my-8">
                <span className="text-5xl font-black text-white">R$ 152</span>
                <span className="text-slate-400 text-sm font-medium ml-1">/mês</span>
              </div>
              <p className="text-slate-400 mb-8 text-sm leading-relaxed border-b border-slate-700 pb-8">Ideal para condomínios de pequeno porte que precisam organizar a rotina básica.</p>
              <ul className="space-y-4 mb-8 flex-1">
                 {benefitsList.map((item, idx) => (
                    <li key={idx} className="flex items-start text-sm text-slate-300">
                      <CheckCircle className="h-5 w-5 text-emerald-500 mr-3 shrink-0" /> <span className="pt-0.5">{item}</span>
                    </li>
                 ))}
              </ul>
              <button 
                onClick={() => navigate('/pricing')}
                className="block w-full bg-slate-700 hover:bg-slate-600 text-white text-center py-4 rounded-xl font-bold transition-all"
              >
                Começar Grátis
              </button>
            </div>

            {/* PLANO 2 - BUSINESS (DESTAQUE) */}
            <div className="bg-slate-800 rounded-3xl p-8 border-2 border-emerald-500 relative transform md:-translate-y-4 shadow-[0_0_40px_rgba(16,185,129,0.15)] flex flex-col z-20">
              <div className="absolute -top-4 left-1/2 -translate-x-1/2 bg-emerald-500 text-slate-900 text-xs font-black px-4 py-1.5 rounded-full uppercase tracking-widest shadow-lg whitespace-nowrap">
                O MAIS ESCOLHIDO
              </div>
              <h3 className="text-2xl font-black text-white">Business</h3>
              <p className="text-xs text-emerald-400 font-bold uppercase tracking-wider mb-4 mt-1">31 a 80 Unidades</p>
              
              <div className="my-8">
                <span className="text-5xl font-black text-white">R$ 279,20</span>
                <span className="text-slate-400 text-sm font-medium ml-1">/mês</span>
              </div>
              <p className="text-slate-300 mb-8 text-sm leading-relaxed border-b border-slate-700 pb-8">A solução completa para o síndico que busca governança, auditoria e automação total.</p>

              <ul className="space-y-4 mb-8 flex-1">
                 {benefitsList.map((item, idx) => (
                    <li key={idx} className="flex items-start text-sm text-white font-medium">
                      <CheckCircle className="h-5 w-5 text-emerald-400 mr-3 shrink-0" /> <span className="pt-0.5">{item}</span>
                    </li>
                 ))}
              </ul>
              
              <button 
                onClick={() => navigate('/pricing')}
                className="block w-full bg-emerald-600 hover:bg-emerald-500 text-white text-center py-4 rounded-xl font-black transition-all shadow-lg shadow-emerald-600/20"
              >
                Começar Grátis
              </button>
            </div>

            {/* PLANO 3 - CUSTOM */}
            <div className="bg-slate-800/40 backdrop-blur-md rounded-3xl p-8 border border-slate-700/50 hover:border-emerald-500/50 hover:bg-slate-800/60 transition-all flex flex-col group">
              <h3 className="text-2xl font-black text-slate-200">Custom</h3>
              <p className="text-xs text-amber-400 font-bold uppercase tracking-wider mb-4 mt-1">Acima de 80 Unidades</p>
              <div className="my-8 flex items-center h-[56px]">
                <span className="text-3xl font-black text-white">Sob Medida</span>
              </div>
              <p className="text-slate-400 mb-8 text-sm leading-relaxed border-b border-slate-700 pb-8">Infraestrutura dedicada e escalabilidade total para grandes empreendimentos e administradoras.</p>
              <ul className="space-y-4 mb-8 flex-1">
                 {benefitsList.map((item, idx) => (
                    <li key={idx} className="flex items-start text-sm text-slate-300">
                      <CheckCircle className="h-5 w-5 text-emerald-500 mr-3 shrink-0" /> <span className="pt-0.5">{item}</span>
                    </li>
                 ))}
                 <li className="text-amber-400/80 text-sm font-bold pl-8 flex items-center gap-2">
                    <Crown className="w-4 h-4"/> Suporte Dedicado
                 </li>
              </ul>
              <button 
                onClick={() => navigate('/pricing')}
                className="block w-full bg-slate-700 hover:bg-slate-600 text-white text-center py-4 rounded-xl font-bold transition-all"
              >
                Falar com Consultor
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================
          DEPOIMENTOS (SOCIAL PROOF)
          ========================================================= */}
      <section className="py-24 bg-slate-50 border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-16">
              <h2 className="text-3xl lg:text-5xl font-black text-slate-900 tracking-tight">Aprovado por quem gere.</h2>
              <p className="text-slate-600 mt-4 max-w-2xl mx-auto text-lg">Veja o que síndicos profissionais e administradoras falam sobre a mudança para o Votzz OS.</p>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {testimonials.map((item, idx) => (
                <div key={idx} className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm relative hover:shadow-xl transition-all duration-300 group">
                  <div className="absolute top-8 right-8 text-slate-100 group-hover:text-emerald-50 transition-colors">
                    <Quote className="w-16 h-16" />
                  </div>
                  <div className="flex gap-1 mb-6 relative z-10">
                    {[1,2,3,4,5].map(i => <Star key={i} className="w-4 h-4 text-amber-400 fill-current" />)}
                  </div>
                  <p className="text-slate-700 text-sm leading-relaxed mb-8 relative z-10 font-medium">
                    "{item.text}"
                  </p>
                  <div className="flex items-center gap-4 relative z-10 pt-6 border-t border-slate-100">
                    <img src={item.avatar} alt={item.name} className="w-12 h-12 rounded-full bg-slate-100" />
                    <div>
                      <p className="font-bold text-slate-900 text-sm">{item.name}</p>
                      <p className="text-[10px] text-emerald-600 font-bold uppercase tracking-wider">{item.role}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
        </div>
      </section>

      {/* =========================================================
          PROGRAMA DE AFILIADOS (IDENTIDADE PRESERVADA)
          ========================================================= */}
      <section id="affiliates" className="py-32 bg-gradient-to-br from-[#0f172a] via-[#1e293b] to-[#0f172a] text-white relative overflow-hidden">
        {/* Decorativos */}
        <div className="absolute top-0 right-0 p-10 opacity-5 pointer-events-none select-none">
            <TrendingUp className="w-[30rem] h-[30rem]" />
        </div>
        <div className="absolute bottom-0 left-0 p-20 opacity-5 pointer-events-none select-none -rotate-12">
            <Percent className="w-64 h-64" />
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
            <div className="flex flex-col lg:flex-row items-center gap-16">
              
              <div className="lg:w-1/2 space-y-10 text-center lg:text-left">
                <div className="inline-flex items-center gap-2 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-4 py-2 rounded-full text-xs font-black uppercase tracking-[0.2em] mb-4">
                    <Gift className="w-4 h-4" /> Programa de Parceiros
                </div>
                
                <h2 className="text-4xl lg:text-[4rem] font-black mb-6 leading-none tracking-tight">
                  Lucre indicando o <span className="text-emerald-400 drop-shadow-[0_0_15px_rgba(16,185,129,0.3)]">Votzz</span>
                </h2>
                
                <div className="space-y-6">
                  <p className="text-xl text-slate-300 leading-relaxed max-w-2xl font-light">
                    Você é síndico profissional, administradora de condomínios ou consultor imobiliário? 
                  </p>
                  
                  <div className="bg-slate-800/50 border-l-4 border-emerald-500 p-6 rounded-r-2xl transform hover:scale-[1.02] transition-transform shadow-lg">
                    <p className="text-2xl lg:text-3xl font-bold text-white leading-tight">
                      Ganhe <span className="text-emerald-400 font-black underline decoration-emerald-500/50 decoration-4 underline-offset-4">30% de comissão</span> por cada venda realizada.
                    </p>
                    <p className="text-slate-400 text-sm mt-4 flex items-center gap-2 justify-center lg:justify-start">
                      <Check className="w-5 h-5 text-emerald-500" /> Receba sua parte assim que o cliente pagar a primeira fatura.
                    </p>
                  </div>
                </div>
                
                <div className="pt-4">
                  <Link 
                    to="/affiliate/register" 
                    className="group relative inline-flex items-center gap-4 bg-emerald-500 hover:bg-emerald-400 text-[#0f172a] font-black px-10 py-5 rounded-full shadow-[0_0_30px_rgba(16,185,129,0.3)] transition-all hover:scale-105 active:scale-95"
                  >
                      QUERO SER UM PARCEIRO AGORA
                      <ArrowRight className="w-5 h-5 group-hover:translate-x-2 transition-transform" />
                  </Link>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-3 gap-6 pt-10 border-t border-slate-700/50">
                    <div className="flex items-center gap-3">
                      <CircleDollarSign className="text-emerald-500 w-6 h-6" />
                      <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">Pagamento PIX</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <Handshake className="text-emerald-500 w-6 h-6" />
                      <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">Apoio em Vendas</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <BarChart3 className="text-emerald-500 w-6 h-6" />
                      <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">Painel de Ganhos</span>
                    </div>
                </div>
              </div>

              {/* Card de Ganhos Visuais */}
              <div className="lg:w-1/2 w-full max-w-lg mx-auto">
                  <div className="bg-slate-800/80 backdrop-blur-xl border border-slate-700/80 rounded-[2.5rem] p-10 lg:p-12 transform hover:rotate-1 hover:scale-[1.01] transition-all duration-700 relative shadow-2xl">
                      
                      <div className="absolute -top-5 -right-5 bg-emerald-500 text-slate-900 font-black px-6 py-3 rounded-2xl text-lg shadow-[0_10px_20px_rgba(16,185,129,0.4)] animate-bounce z-20">
                        30% POR VENDA
                      </div>

                      <div className="flex items-center justify-between mb-10 border-b border-slate-700 pb-8">
                        <div>
                           <p className="text-sm font-bold text-slate-400 uppercase tracking-widest mb-2">Ganhos Estimados</p>
                           <p className="text-5xl lg:text-6xl font-black text-white tracking-tighter">R$ 8.500</p>
                        </div>
                        <div className="bg-emerald-500/20 p-5 rounded-3xl border border-emerald-500/30 shadow-inner">
                           <TrendingUp className="w-10 h-10 text-emerald-500" />
                        </div>
                      </div>

                      <div className="space-y-4">
                        <p className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-4">Vendas Recentes</p>
                        
                        <div className="flex justify-between items-center bg-slate-900/60 p-5 rounded-2xl border border-slate-700/50 hover:border-emerald-500/30 transition-colors">
                          <div className="flex items-center gap-4">
                            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_10px_#10b981]"></div>
                            <span className="text-slate-200 font-bold">Condomínio Jardins</span>
                          </div>
                          <span className="text-emerald-400 font-black text-lg">+ R$ 1.008</span>
                        </div>

                        <div className="flex justify-between items-center bg-slate-900/60 p-5 rounded-2xl border border-slate-700/50 hover:border-emerald-500/30 transition-colors">
                          <div className="flex items-center gap-4">
                            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_10px_#10b981]"></div>
                            <span className="text-slate-200 font-bold">Clube Pinheiros</span>
                          </div>
                          <span className="text-emerald-400 font-black text-lg">+ R$ 594</span>
                        </div>

                        <div className="flex justify-between items-center bg-slate-900/60 p-5 rounded-2xl border border-slate-700/50 hover:border-emerald-500/30 transition-colors">
                          <div className="flex items-center gap-4">
                            <div className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_10px_#10b981]"></div>
                            <span className="text-slate-200 font-bold">Residencial Orion</span>
                          </div>
                          <span className="text-emerald-400 font-black text-lg">+ R$ 288</span>
                        </div>
                      </div>

                      <div className="mt-10 text-center">
                        <p className="text-[10px] text-slate-500 font-bold uppercase tracking-[0.3em]">Sistema Transparente Votzz Partner</p>
                      </div>
                  </div>
              </div>
            </div>

            {/* FAQ Afiliados */}
            <div className="mt-32 max-w-5xl mx-auto border-t border-slate-800 pt-16">
                <div className="text-center mb-12">
                  <h3 className="text-2xl font-bold text-white">Dúvidas sobre o programa?</h3>
                </div>
                <div className="grid md:grid-cols-3 gap-10">
                  {affiliateFaq.map((item, idx) => (
                    <div key={idx} className="bg-slate-800/30 p-6 rounded-2xl border border-slate-700/50">
                      <h4 className="font-bold text-emerald-400 mb-3 text-lg">{item.q}</h4>
                      <p className="text-sm text-slate-400 leading-relaxed">{item.a}</p>
                    </div>
                  ))}
                </div>
            </div>
        </div>
      </section>

      {/* =========================================================
          FAQ GERAL
          ========================================================= */}
      <section className="py-24 bg-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl lg:text-5xl font-black text-slate-900 mb-4 tracking-tight">Perguntas Frequentes</h2>
            <p className="text-slate-600 text-lg">Tudo o que você precisa saber antes de digitalizar sua gestão.</p>
          </div>

          <div className="space-y-4">
            {generalFaq.map((faq, index) => (
              <div 
                key={index} 
                className={`border rounded-2xl transition-all duration-300 ${activeFaq === index ? 'border-emerald-500 shadow-md bg-emerald-50/30' : 'border-slate-200 bg-white hover:border-slate-300'}`}
              >
                <button 
                  className="w-full text-left px-6 py-5 flex justify-between items-center outline-none focus:ring-2 focus:ring-emerald-500 rounded-2xl"
                  onClick={() => setActiveFaq(activeFaq === index ? null : index)}
                >
                  <span className="font-bold text-slate-900 pr-8">{faq.q}</span>
                  {activeFaq === index ? (
                    <ChevronDown className="w-5 h-5 text-emerald-500 shrink-0 transform rotate-180 transition-transform" />
                  ) : (
                    <Plus className="w-5 h-5 text-slate-400 shrink-0 transition-transform" />
                  )}
                </button>
                {activeFaq === index && (
                  <div className="px-6 pb-6 text-slate-600 text-sm leading-relaxed animate-in slide-in-from-top-1 fade-in duration-200">
                    {faq.a}
                  </div>
                )}
              </div>
            ))}
          </div>
          
          <div className="mt-12 text-center bg-slate-50 p-6 rounded-2xl border border-slate-200">
             <p className="text-slate-600 font-medium mb-3">Ainda tem dúvidas?</p>
             <a href="mailto:suporte@votzz.com.br" className="text-emerald-600 font-bold hover:text-emerald-700 underline underline-offset-4 inline-block">
               Fale com um especialista agora mesmo.
             </a>
          </div>
        </div>
      </section>

      {/* =========================================================
          CTA FINAL (A PROVOCAÇÃO ESTRATÉGICA)
          ========================================================= */}
      <section id="demo" className="py-32 bg-emerald-600 text-center px-4 relative overflow-hidden">
        {/* Background Animation */}
        <div className="absolute inset-0 bg-emerald-700 opacity-40 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-emerald-400 via-transparent to-transparent"></div>
        
        {/* Decorative pattern */}
        <div className="absolute inset-0" style={{ backgroundImage: 'radial-gradient(circle at 2px 2px, rgba(255,255,255,0.15) 1px, transparent 0)', backgroundSize: '24px 24px' }}></div>

        <div className="relative z-10 max-w-4xl mx-auto space-y-10">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-emerald-500/30 border border-emerald-400/50 mb-4 backdrop-blur-sm">
             <AlertCircle className="w-10 h-10 text-white" />
          </div>
          <h2 className="text-4xl lg:text-6xl font-black text-white leading-tight tracking-tight">
            Seu condomínio ainda funciona no improviso?
          </h2>
          <p className="text-emerald-100 text-xl lg:text-2xl max-w-2xl mx-auto font-medium leading-relaxed">
            Digitalize a gestão, centralize a comunicação e profissionalize a rotina. Junte-se a centenas de condomínios modernos.
          </p>
          
          <div className="flex flex-col sm:flex-row justify-center gap-6 pt-6">
              <button 
                onClick={() => navigate('/pricing')}
                className="bg-white text-emerald-800 px-12 py-5 rounded-full font-black text-lg shadow-[0_0_40px_rgba(255,255,255,0.3)] hover:bg-slate-50 transition-all hover:scale-105 active:scale-95"
              >
                SOLICITAR DEMONSTRAÇÃO
              </button>
              <button 
                onClick={() => navigate('/pricing')}
                className="bg-emerald-800/40 text-white border-2 border-emerald-400/50 px-12 py-5 rounded-full font-black text-lg backdrop-blur-md hover:bg-emerald-800/60 transition-all"
              >
                VER PLANOS E PREÇOS
              </button>
          </div>
        </div>
      </section>

      {/* =========================================================
          FOOTER
          ========================================================= */}
      <footer id="contact" className="bg-[#0a0f1c] text-slate-400 py-24 relative border-t border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-12 lg:gap-8 mb-16">
              
              <div className="lg:col-span-2 space-y-8 text-center md:text-left pr-0 lg:pr-10">
                  <Link to="/" className="inline-block flex justify-center md:justify-start">
                    <Logo theme="light" showSlogan={true} />
                  </Link>
                  <p className="text-sm leading-relaxed max-w-sm mx-auto md:mx-0">
                    A Votzz é pioneira em tecnologia para gestão centralizada, transparente e eficiente em comunidades modernos. O verdadeiro sistema operacional do seu condomínio.
                  </p>
                  <div className="flex justify-center md:justify-start gap-4">
                    <a href="mailto:suporte@votzz.com.br" className="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center hover:bg-emerald-600 hover:text-white transition-colors"><Mail size={18}/></a>
                    <a href="#" className="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center hover:bg-emerald-600 hover:text-white transition-colors"><Smartphone size={18}/></a>
                  </div>
              </div>

              <div className="text-center md:text-left">
                  <h4 className="text-white font-black text-sm mb-6 uppercase tracking-widest">Produto</h4>
                  <ul className="space-y-4 text-sm font-medium">
                    <li><button onClick={() => scrollToSection('plataforma')} className="hover:text-emerald-400 transition-colors">Funcionalidades</button></li>
                    <li><button onClick={() => navigate('/pricing')} className="hover:text-emerald-400 transition-colors">Planos e Preços</button></li>
                    <li><button onClick={() => scrollToSection('como-funciona')} className="hover:text-emerald-400 transition-colors">Como Implantar</button></li>
                    <li><Link to="/app" className="hover:text-emerald-400 transition-colors">Baixar App Mobile</Link></li>
                  </ul>
              </div>

              <div className="text-center md:text-left">
                  <h4 className="text-white font-black text-sm mb-6 uppercase tracking-widest">Empresa</h4>
                  <ul className="space-y-4 text-sm font-medium">
                    <li><Link to="/sobre" className="hover:text-emerald-400 transition-colors">Quem Somos</Link></li>
                    <li><button onClick={() => scrollToSection('affiliates')} className="hover:text-emerald-400 transition-colors flex items-center gap-2 justify-center md:justify-start">Seja um Parceiro <span className="bg-emerald-500/20 text-emerald-400 px-2 py-0.5 rounded text-[10px] font-bold">GANHE 30%</span></button></li>
                    <li><Link to="/blog" className="hover:text-emerald-400 transition-colors">Blog da Gestão</Link></li>
                  </ul>
              </div>

              <div className="text-center md:text-left">
                  <h4 className="text-white font-black text-sm mb-6 uppercase tracking-widest">Contato e Legal</h4>
                  <ul className="space-y-4 text-sm font-medium">
                    <li className="flex items-center justify-center md:justify-start gap-3 text-emerald-400">
                      <Mail size={16} /> suporte@votzz.com.br
                    </li>
                    <li className="flex items-center justify-center md:justify-start gap-3 text-emerald-400">
                      <Headphones size={16} /> Chat Online (Seg-Sex)
                    </li>
                    <li className="pt-4 border-t border-slate-800 mt-4 flex flex-col items-center md:items-start">
                      <Link to="/terms" className="hover:text-white transition-colors mb-2 inline-block">Termos de Uso</Link>
                      <Link to="/privacy" className="hover:text-white transition-colors inline-block">Política de Privacidade</Link>
                    </li>
                  </ul>
              </div>
            </div>

            <div className="border-t border-slate-800 pt-8 flex flex-col md:flex-row justify-between items-center gap-6">
                <div className="flex flex-col items-center md:items-start">
                  <p className="text-xs text-slate-500">
                    &copy; {new Date().getFullYear()} Votzz Technology. Feito para condomínios inteligentes.
                  </p>
                  <p className="text-[10px] text-slate-600 mt-1">
                    B & M NEGOCIOS LTDA • CNPJ: 58.500.491/0001-10
                  </p>
                </div>
                
                <div className="flex items-center gap-3 bg-slate-800/50 px-4 py-2 rounded-full border border-slate-700">
                  <ShieldCheck className="w-5 h-5 text-emerald-500" />
                  <div className="flex flex-col">
                    <span className="text-[10px] font-black text-white uppercase leading-none">Proteção de Dados</span>
                    <span className="text-[9px] font-bold text-slate-400 uppercase leading-none mt-0.5">LGPD Compliant</span>
                  </div>
                </div>
            </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
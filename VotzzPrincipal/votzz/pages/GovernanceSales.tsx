
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  CheckSquare, 
  Megaphone, 
  Calendar as CalendarIcon, 
  ShieldCheck, 
  TrendingUp, 
  Zap, 
  Users, 
  Smartphone, 
  Lock,
  Menu,
  X,
  LayoutDashboard,
  CheckCircle,
  ArrowRight,
  Star,
  MessageCircle,
  CalendarDays
} from 'lucide-react';
import { Logo } from '../components/Logo';
import { User } from '../types';

interface GovernanceSalesProps {
  user: User | null;
}

const GovernanceSales: React.FC<GovernanceSalesProps> = ({ user }) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      {/* 1. Header (Shared) */}
      <header 
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          isScrolled ? 'bg-white/95 backdrop-blur-sm shadow-sm py-3' : 'bg-slate-900 text-white py-4'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 flex justify-between items-center">
          <Link to="/" className="cursor-pointer hover:opacity-80 transition-opacity">
            <Logo theme={isScrolled ? "dark" : "light"} />
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center space-x-8">
             <Link to="/pricing" className={`text-sm font-medium hover:text-emerald-500 transition-colors ${isScrolled ? 'text-slate-600' : 'text-slate-300'}`}>Planos</Link>
             <Link to="/blog" className={`text-sm font-medium hover:text-emerald-500 transition-colors ${isScrolled ? 'text-slate-600' : 'text-slate-300'}`}>Blog</Link>
             <Link to="/faq" className={`text-sm font-medium hover:text-emerald-500 transition-colors ${isScrolled ? 'text-slate-600' : 'text-slate-300'}`}>Ajuda</Link>
          </nav>

          <div className="hidden md:flex items-center space-x-4">
            {user ? (
              <Link 
                to="/dashboard" 
                className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2.5 rounded-full text-sm font-bold transition-all shadow-md flex items-center gap-2"
              >
                <LayoutDashboard className="w-4 h-4" />
                Meu Dashboard
              </Link>
            ) : (
              <Link 
                to="/login" 
                className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2.5 rounded-full text-sm font-bold transition-all shadow-md"
              >
                Entrar
              </Link>
            )}
          </div>

           <button className="md:hidden p-2" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
             {mobileMenuOpen ? <X className={isScrolled ? 'text-slate-800' : 'text-white'} /> : <Menu className={isScrolled ? 'text-slate-800' : 'text-white'} />}
           </button>
        </div>
      </header>

      {/* 2. Hero Section */}
      <section className="pt-32 pb-20 bg-slate-900 text-white relative overflow-hidden">
        <div className="absolute inset-0 bg-emerald-600/10"></div>
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-purple-500 rounded-full blur-[128px] opacity-20"></div>
        
        <div className="max-w-7xl mx-auto px-4 text-center relative z-10">
          <div className="inline-flex items-center gap-2 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide mb-8">
             <Star className="w-4 h-4 fill-current" /> Exclusividade do Plano Anual
          </div>
          <h1 className="text-4xl md:text-6xl font-extrabold mb-6 leading-tight">
             Governança Digital <span className="text-emerald-400">Premium</span>
          </h1>
          <p className="text-xl text-slate-300 max-w-3xl mx-auto mb-10 leading-relaxed">
             Eleve o nível da sua gestão. Tenha acesso a ferramentas avançadas de micro-decisões, <strong>reserva de áreas comuns</strong>, comunicados oficiais e organização completa que só o Plano Anual oferece.
          </p>
          <div className="flex flex-col sm:flex-row justify-center gap-4">
             <Link to="/pricing" className="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-4 rounded-full font-bold shadow-[0_0_20px_rgba(16,185,129,0.4)] transition-all hover:scale-105">
                Quero o Plano Anual
             </Link>
             <button onClick={() => document.getElementById('benefits')?.scrollIntoView({behavior: 'smooth'})} className="bg-white/10 hover:bg-white/20 text-white px-8 py-4 rounded-full font-bold border border-white/10 transition-colors">
                Conhecer Benefícios
             </button>
          </div>
        </div>
      </section>

      {/* 3. The Core Features */}
      <section className="py-20 -mt-10 relative z-20 px-4">
        <div className="max-w-7xl mx-auto grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            <div className="bg-white p-8 rounded-2xl shadow-xl border border-slate-100 flex flex-col items-center text-center transform hover:-translate-y-1 transition-transform">
               <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-2xl flex items-center justify-center mb-6">
                  <CheckSquare className="w-8 h-8" />
               </div>
               <h3 className="text-xl font-bold text-slate-900 mb-3">Micro-decisões Ágeis</h3>
               <p className="text-slate-600 text-sm">
                  Pare de convocar assembleias para tudo. Aprove compras pequenas, cores de pintura e regras simples através de enquetes rápidas.
               </p>
            </div>
            
            <div className="bg-white p-8 rounded-2xl shadow-xl border border-slate-100 flex flex-col items-center text-center transform hover:-translate-y-1 transition-transform">
               <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-2xl flex items-center justify-center mb-6">
                  <Megaphone className="w-8 h-8" />
               </div>
               <h3 className="text-xl font-bold text-slate-900 mb-3">Mural Oficial Digital</h3>
               <p className="text-slate-600 text-sm">
                  Substitua o papel no elevador. Envie comunicados com confirmação de leitura obrigatória e garanta a ciência de todos.
               </p>
            </div>

            <div className="bg-white p-8 rounded-2xl shadow-xl border border-slate-100 flex flex-col items-center text-center transform hover:-translate-y-1 transition-transform">
               <div className="w-16 h-16 bg-orange-100 text-orange-600 rounded-2xl flex items-center justify-center mb-6">
                  <CalendarDays className="w-8 h-8" />
               </div>
               <h3 className="text-xl font-bold text-slate-900 mb-3">Reservas de Espaços</h3>
               <p className="text-slate-600 text-sm">
                  Agendamento de salão de festas, churrasqueiras e áreas comuns com controle automático de disponibilidade e regras.
               </p>
            </div>

            <div className="bg-white p-8 rounded-2xl shadow-xl border border-slate-100 flex flex-col items-center text-center transform hover:-translate-y-1 transition-transform">
               <div className="w-16 h-16 bg-purple-100 text-purple-600 rounded-2xl flex items-center justify-center mb-6">
                  <CalendarIcon className="w-8 h-8" />
               </div>
               <h3 className="text-xl font-bold text-slate-900 mb-3">Calendário Inteligente</h3>
               <p className="text-slate-600 text-sm">
                  Centralize manutenções, vencimentos de mandatos, festas e assembleias em um único lugar acessível a todos.
               </p>
            </div>
        </div>
      </section>

      {/* 4. Why Annual Plan? (The List) */}
      <section id="benefits" className="py-20 bg-white">
         <div className="max-w-7xl mx-auto px-4">
            <div className="text-center mb-16">
               <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mb-4">Por que assinar o Plano Anual?</h2>
               <p className="text-lg text-slate-600 max-w-2xl mx-auto">
                  Mais do que economia, uma escolha estratégica para a saúde administrativa do seu condomínio.
               </p>
            </div>

            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
               {[
                  {
                     title: "Funcionalidades Avançadas",
                     desc: "Acesso irrestrito a todo o ecossistema de Governança Digital, incluindo reservas e enquetes.",
                     icon: Zap,
                     color: "text-amber-500"
                  },
                  {
                     title: "Descontos Exclusivos",
                     desc: "Economia de mais de 30% em relação ao plano mensal, permitindo investir em outras melhorias.",
                     icon: TrendingUp,
                     color: "text-emerald-600"
                  },
                  {
                     title: "Conformidade Legal",
                     desc: "Selo de Governança e ferramentas atualizadas constantemente com a LGPD e o Código Civil.",
                     icon: ShieldCheck,
                     color: "text-blue-600"
                  },
                  {
                     title: "Transparência Total",
                     desc: "Histórico vitalício de todas as decisões, facilitando auditorias e a prestação de contas do síndico.",
                     icon: Users,
                     color: "text-purple-600"
                  },
                  {
                     title: "Automação e Eficiência",
                     desc: "Menos burocracia manual. O sistema cuida dos avisos, reservas, apurações e registros para você.",
                     icon: LayoutDashboard,
                     color: "text-indigo-600"
                  },
                  {
                     title: "Suporte via WhatsApp",
                     desc: "Atendimento VIP em tempo real. Tire dúvidas e resolva pendências direto no WhatsApp do seu gerente.",
                     icon: MessageCircle,
                     color: "text-green-500"
                  },
                  {
                     title: "Implementação Fácil",
                     desc: "Acompanhamento passo a passo para cadastrar moradores e iniciar o uso sem dores de cabeça.",
                     icon: CheckSquare,
                     color: "text-cyan-600"
                  },
                  {
                     title: "Mobilidade Completa",
                     desc: "Acesso total via smartphone para síndicos e moradores agendarem espaços e votarem de qualquer lugar.",
                     icon: Smartphone,
                     color: "text-slate-600"
                  }
               ].map((item, idx) => (
                  <div key={idx} className="bg-slate-50 p-6 rounded-xl border border-slate-100 hover:bg-emerald-50/30 transition-colors">
                     <item.icon className={`w-10 h-10 mb-4 ${item.color}`} />
                     <h3 className="font-bold text-slate-900 mb-2">{item.title}</h3>
                     <p className="text-sm text-slate-600 leading-relaxed">{item.desc}</p>
                  </div>
               ))}
            </div>
         </div>
      </section>

      {/* 5. Pricing CTA Hook */}
      <section className="py-20 bg-slate-900 text-white relative overflow-hidden">
         <div className="absolute top-0 right-0 w-full h-full bg-[radial-gradient(circle_at_top_right,_var(--tw-gradient-stops))] from-emerald-900/40 via-slate-900 to-slate-900 pointer-events-none"></div>
         <div className="max-w-4xl mx-auto px-4 text-center relative z-10">
            <h2 className="text-3xl md:text-5xl font-bold mb-8">Modernize sua gestão hoje</h2>
            <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-2xl p-8 md:p-12 mb-8">
               <div className="flex flex-col md:flex-row items-center justify-between gap-8">
                  <div className="text-left">
                     <p className="text-emerald-400 font-bold uppercase tracking-wide text-sm mb-2">Plano Anual Votzz</p>
                     <div className="text-5xl font-extrabold mb-2">R$ 203<span className="text-xl text-slate-400 font-normal">/mês</span></div>
                     <p className="text-slate-400 text-sm">Faturado anualmente (R$ 2.436). Economia de R$ 1.044/ano.</p>
                  </div>
                  <div className="flex flex-col gap-3 w-full md:w-auto">
                     <Link to="/login" state={{ isRegister: true }} className="bg-emerald-600 hover:bg-emerald-500 text-white px-8 py-4 rounded-xl font-bold text-lg shadow-lg hover:shadow-emerald-500/20 transition-all text-center">
                        Assinar Agora
                     </Link>
                     <p className="text-xs text-slate-500 flex items-center justify-center gap-1">
                        <Lock className="w-3 h-3" /> Pagamento Seguro e Garantia de 7 dias
                     </p>
                  </div>
               </div>
               <div className="mt-8 pt-8 border-t border-white/10 grid grid-cols-2 md:grid-cols-4 gap-4 text-left">
                  <div className="flex items-center gap-2 text-sm text-slate-300"><CheckCircle className="w-4 h-4 text-emerald-500" /> Votos Ilimitados</div>
                  <div className="flex items-center gap-2 text-sm text-slate-300"><CheckCircle className="w-4 h-4 text-emerald-500" /> Reservas Online</div>
                  <div className="flex items-center gap-2 text-sm text-slate-300"><CheckCircle className="w-4 h-4 text-emerald-500" /> App do Morador</div>
                  <div className="flex items-center gap-2 text-sm text-slate-300"><MessageCircle className="w-4 h-4 text-emerald-500" /> Suporte WhatsApp</div>
               </div>
            </div>
         </div>
      </section>

      {/* 6. Footer (Shared) */}
      <footer className="bg-slate-950 text-slate-400 py-12 border-t border-slate-800">
        <div className="max-w-7xl mx-auto px-4 text-center md:text-left grid md:grid-cols-4 gap-8">
            <div>
               <Logo theme="light" />
               <p className="text-sm mt-4">Plataforma líder em democracia condominial.</p>
            </div>
            <div>
               <h4 className="text-white font-bold mb-4">Produto</h4>
               <ul className="space-y-2 text-sm">
                  <li><Link to="/pricing" className="hover:text-white">Planos</Link></li>
                  <li><Link to="/governance-solutions" className="text-emerald-500 font-bold">Governança Digital</Link></li>
                  <li><Link to="/" className="hover:text-white">Assembleias</Link></li>
               </ul>
            </div>
            <div>
               <h4 className="text-white font-bold mb-4">Suporte</h4>
               <ul className="space-y-2 text-sm">
                  <li><Link to="/faq" className="hover:text-white">Central de Ajuda</Link></li>
               </ul>
            </div>
            <div>
               <h4 className="text-white font-bold mb-4">Legal</h4>
               <ul className="space-y-2 text-sm">
                  <li><Link to="/terms" className="hover:text-white">Termos de Uso</Link></li>
                  <li><Link to="/privacy" className="hover:text-white">Privacidade</Link></li>
                  <li><Link to="/compliance" className="hover:text-white">Compliance</Link></li>
               </ul>
            </div>
        </div>
      </footer>
    </div>
  );
};

export default GovernanceSales;

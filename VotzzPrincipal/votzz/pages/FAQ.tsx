
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Menu, 
  X, 
  ChevronDown, 
  ChevronUp, 
  Search, 
  HelpCircle, 
  ShieldCheck, 
  Gavel, 
  Smartphone 
} from 'lucide-react';
import { Logo } from '../components/Logo';

const FaqHeader: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${isScrolled ? 'bg-white shadow-sm py-3' : 'bg-slate-900 text-white py-4'}`}>
      <div className="max-w-7xl mx-auto px-4 flex justify-between items-center">
        <Link to="/" className="hover:opacity-80 transition-opacity">
          <Logo theme={isScrolled ? "dark" : "light"} />
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center space-x-8">
          <Link to="/pricing" className={`text-sm font-medium ${isScrolled ? 'text-slate-600 hover:text-emerald-600' : 'text-slate-300 hover:text-white'}`}>Preços</Link>
          <Link to="/blog" className={`text-sm font-medium ${isScrolled ? 'text-slate-600 hover:text-emerald-600' : 'text-slate-300 hover:text-white'}`}>Blog</Link>
          <Link to="/faq" className={`text-sm font-bold ${isScrolled ? 'text-emerald-600' : 'text-white'}`}>FAQ</Link>
        </nav>

        <div className="hidden md:flex items-center gap-4">
          <Link to="/login" className={`text-sm font-medium ${isScrolled ? 'text-emerald-600' : 'text-emerald-400'}`}>Entrar</Link>
          <Link to="/login" state={{ isRegister: true }} className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2 rounded-full text-sm font-bold transition-all shadow-md">
            Começar Agora
          </Link>
        </div>

        <button className="md:hidden p-2" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
          {mobileMenuOpen ? <X className={isScrolled ? 'text-slate-800' : 'text-white'} /> : <Menu className={isScrolled ? 'text-slate-800' : 'text-white'} />}
        </button>
      </div>

       {/* Mobile Menu */}
       {mobileMenuOpen && (
          <div className="md:hidden absolute top-full left-0 right-0 bg-white border-t border-slate-100 shadow-lg p-4 flex flex-col space-y-4 animate-in slide-in-from-top-2 text-slate-800">
            <Link to="/pricing" className="py-2 border-b border-slate-50">Preços</Link>
            <Link to="/blog" className="py-2 border-b border-slate-50">Blog</Link>
            <Link to="/faq" className="py-2 font-bold text-emerald-600 border-b border-slate-50">FAQ</Link>
            <Link to="/login" className="py-2 text-emerald-600">Entrar</Link>
          </div>
        )}
    </header>
  );
};

const AccordionItem: React.FC<{ question: string; answer: string }> = ({ question, answer }) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="border border-slate-200 rounded-lg bg-white overflow-hidden mb-3 shadow-sm hover:shadow-md transition-shadow">
      <button 
        className="w-full px-6 py-4 flex items-center justify-between text-left focus:outline-none bg-white hover:bg-slate-50 transition-colors"
        onClick={() => setIsOpen(!isOpen)}
      >
        <span className="font-bold text-slate-800">{question}</span>
        {isOpen ? <ChevronUp className="text-emerald-600" /> : <ChevronDown className="text-slate-400" />}
      </button>
      <div className={`transition-all duration-300 ease-in-out ${isOpen ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'}`}>
        <div className="px-6 pb-6 text-slate-600 text-sm leading-relaxed border-t border-slate-100 pt-4">
          {answer}
        </div>
      </div>
    </div>
  );
};

const FAQ: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const categories = [
    {
      title: "Legal e Segurança",
      icon: Gavel,
      questions: [
        { q: "A votação online tem validade jurídica?", a: "Sim. A Lei 14.309/2022 alterou o Código Civil para permitir expressamente a realização de assembleias e votações por meios eletrônicos, desde que garantam a identificação dos participantes e a segurança do voto." },
        { q: "O sistema é seguro contra fraudes?", a: "Utilizamos criptografia de ponta (SSL/TLS) e hashing SHA-256 para cada voto registrado. Isso cria uma trilha de auditoria imutável, garantindo que um voto não possa ser alterado após computado." },
        { q: "Como é feita a identificação do morador?", a: "A plataforma exige cadastro prévio com validação de dados (CPF, E-mail e Unidade). Opcionalmente, o administrador pode ativar a autenticação de dois fatores (2FA) para maior segurança no login." }
      ]
    },
    {
      title: "Uso da Plataforma",
      icon: Smartphone,
      questions: [
        { q: "Preciso baixar algum aplicativo?", a: "Não. O Votzz é uma plataforma web responsiva (PWA). Você pode acessar pelo navegador do seu celular, tablet ou computador sem instalar nada." },
        { q: "Posso mudar meu voto?", a: "Depende da configuração da assembleia. Normalmente, o voto é definitivo para garantir a integridade da apuração parcial. Se o administrador permitir 're-voto', a ação ficará disponível até o encerramento da pauta." },
        { q: "O que acontece se eu esquecer minha senha?", a: "Na tela de login, clique em 'Esqueci minha senha'. Enviaremos um link de recuperação para o e-mail cadastrado." }
      ]
    },
    {
      title: "Financeiro e Planos",
      icon: ShieldCheck,
      questions: [
        { q: "Como funciona o pagamento do plano avulso?", a: "O plano avulso é pago uma única vez via cartão de crédito ou PIX, garantindo acesso total à plataforma para a realização de uma única assembleia (com duração de até 30 dias)." },
        { q: "Há fidelidade no plano mensal?", a: "Não. Você pode cancelar a assinatura mensal a qualquer momento através do painel administrativo, sem multas. O acesso continua ativo até o fim do ciclo pago." },
        { q: "Oferecem nota fiscal?", a: "Sim, a nota fiscal é emitida automaticamente para o CNPJ do condomínio ou CPF do administrador após a confirmação do pagamento." }
      ]
    }
  ];

  const filteredCategories = categories.map(cat => ({
    ...cat,
    questions: cat.questions.filter(
      item => 
        item.q.toLowerCase().includes(searchTerm.toLowerCase()) || 
        item.a.toLowerCase().includes(searchTerm.toLowerCase())
    )
  })).filter(cat => cat.questions.length > 0);

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      <FaqHeader />

      {/* Hero Section */}
      <section className="pt-32 pb-20 bg-slate-900 text-white text-center px-4 relative overflow-hidden">
        <div className="absolute inset-0 bg-emerald-600/10 opacity-30"></div>
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-purple-500 rounded-full blur-[128px] opacity-20"></div>
        <div className="absolute top-20 right-20 w-72 h-72 bg-emerald-500 rounded-full blur-[100px] opacity-20"></div>
        
        <h1 className="text-4xl md:text-5xl font-extrabold mb-6 relative z-10">
          Central de Ajuda
        </h1>
        <p className="text-xl text-slate-300 max-w-2xl mx-auto relative z-10 mb-8">
          Tire suas dúvidas sobre legalidade, segurança e funcionamento do Votzz.
        </p>

        <div className="max-w-xl mx-auto relative z-10">
          <div className="relative">
            <Search className="absolute left-4 top-3.5 h-5 w-5 text-slate-400" />
            <input 
              type="text" 
              placeholder="Qual sua dúvida?"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3 rounded-full text-slate-800 focus:outline-none focus:ring-4 focus:ring-emerald-500/30 shadow-lg"
            />
          </div>
        </div>
      </section>

      {/* FAQ Content */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto">
          {filteredCategories.length > 0 ? (
            filteredCategories.map((cat, idx) => (
              <div key={idx} className="mb-12">
                <div className="flex items-center gap-3 mb-6">
                  <div className="p-2 bg-emerald-100 rounded-lg">
                    <cat.icon className="h-6 w-6 text-emerald-600" />
                  </div>
                  <h2 className="text-2xl font-bold text-slate-900">{cat.title}</h2>
                </div>
                <div>
                  {cat.questions.map((item, qIdx) => (
                    <AccordionItem key={qIdx} question={item.q} answer={item.a} />
                  ))}
                </div>
              </div>
            ))
          ) : (
             <div className="text-center py-12">
                <p className="text-slate-500 text-lg">Nenhum resultado encontrado para "{searchTerm}".</p>
                <button 
                  onClick={() => setSearchTerm('')} 
                  className="mt-4 text-emerald-600 font-bold hover:underline"
                >
                  Limpar busca
                </button>
             </div>
          )}
        </div>
      </section>

       {/* Still need help? */}
       <section className="py-16 bg-white border-t border-slate-100">
         <div className="max-w-4xl mx-auto px-4 text-center">
            <HelpCircle className="w-12 h-12 text-slate-300 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-slate-900 mb-2">Ainda precisa de ajuda?</h2>
            <p className="text-slate-500 mb-8">Nossa equipe de suporte está disponível para atender síndicos e administradores.</p>
            <div className="flex justify-center gap-4">
               <a href="mailto:suporte@votzz.com.br" className="bg-slate-900 text-white px-6 py-3 rounded-lg font-bold hover:bg-slate-800 transition-colors">
                 Falar com Suporte
               </a>
               <Link to="/contact" className="bg-white border border-slate-300 text-slate-700 px-6 py-3 rounded-lg font-bold hover:bg-slate-50 transition-colors">
                 Ver Tutoriais
               </Link>
            </div>
         </div>
       </section>

      {/* Footer */}
      <footer className="bg-slate-900 text-slate-400 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
           <div className="grid md:grid-cols-4 gap-8 mb-8">
              <div>
                 <div className="mb-4">
                    <Link to="/">
                        <Logo theme="light" />
                    </Link>
                 </div>
                 <p className="text-sm">Tecnologia para decisões democráticas e transparentes.</p>
              </div>
              
              <div>
                 <h4 className="text-white font-bold mb-4">Empresa</h4>
                 <ul className="space-y-2 text-sm">
                    <li><Link to="/pricing" className="hover:text-white">Preços</Link></li>
                    <li><Link to="/blog" className="hover:text-white">Blog</Link></li>
                    <li><Link to="/faq" className="hover:text-white">FAQ</Link></li>
                    <li><Link to="/testimonials" className="hover:text-white">Depoimentos</Link></li>
                 </ul>
              </div>

              <div>
                 <h4 className="text-white font-bold mb-4">Legal</h4>
                 <ul className="space-y-2 text-sm">
                    <li><Link to="/terms" className="hover:text-white">Termos de Uso</Link></li>
                    <li><Link to="/privacy" className="text-emerald-500 font-bold">Privacidade</Link></li>
                    <li><Link to="/compliance" className="hover:text-white">Compliance</Link></li>
                 </ul>
              </div>

              <div>
                 <h4 className="text-white font-bold mb-4">Contato</h4>
                 <ul className="space-y-2 text-sm">
                    <li>suporte@votzz.com.br</li>
                 </ul>
              </div>
           </div>
           <div className="border-t border-slate-800 pt-8 text-center text-xs">
              &copy; {new Date().getFullYear()} Votzz. Todos os direitos reservados.
           </div>
        </div>
      </footer>
    </div>
  );
};

export default FAQ;

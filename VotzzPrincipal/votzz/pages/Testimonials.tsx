
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Menu, 
  X, 
  Star, 
  Quote, 
  User, 
  Building2,
  ArrowRight
} from 'lucide-react';
import { Logo } from '../components/Logo';

const TestimonialsHeader: React.FC = () => {
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
          <Link to="/testimonials" className={`text-sm font-bold ${isScrolled ? 'text-emerald-600' : 'text-white'}`}>Depoimentos</Link>
          <Link to="/blog" className={`text-sm font-medium ${isScrolled ? 'text-slate-600 hover:text-emerald-600' : 'text-slate-300 hover:text-white'}`}>Blog</Link>
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
            <Link to="/testimonials" className="py-2 font-bold text-emerald-600 border-b border-slate-50">Depoimentos</Link>
            <Link to="/blog" className="py-2 border-b border-slate-50">Blog</Link>
            <Link to="/login" className="py-2 text-emerald-600">Entrar</Link>
          </div>
        )}
    </header>
  );
};

const Testimonials: React.FC = () => {
  const reviews = [
    {
      id: 1,
      name: "Roberto Silva",
      role: "Síndico Profissional",
      location: "São Paulo, SP",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Roberto",
      text: "A plataforma revolucionou nossas assembleias. O quórum aumentou de 20% para 85% já na primeira votação online. A facilidade de uso para os idosos foi o que mais me surpreendeu.",
      rating: 5
    },
    {
      id: 2,
      name: "Ana Clara",
      role: "Gerente de Condomínio",
      location: "Rio de Janeiro, RJ",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Ana",
      text: "Administro 4 torres e a gestão de papelada era um pesadelo. Com o Votzz, a ata é gerada automaticamente e a auditoria jurídica me dá total segurança contra impugnações.",
      rating: 5
    },
    {
      id: 3,
      name: "Carlos Mendes",
      role: "Morador e Conselheiro",
      location: "Curitiba, PR",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Carlos",
      text: "Como morador que viaja muito, eu nunca conseguia participar. Agora voto do hotel ou do aeroporto. A interface é muito limpa e transparente.",
      rating: 5
    },
    {
      id: 4,
      name: "Mariana Costa",
      role: "Síndica Orgânica",
      location: "Belo Horizonte, MG",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Mariana",
      text: "Estávamos gastando uma fortuna com aluguel de cadeiras e som para reuniões vazias. O custo da plataforma se pagou na primeira assembleia apenas com a economia de infraestrutura.",
      rating: 4
    },
    {
      id: 5,
      name: "Dr. Ricardo Oliveira",
      role: "Advogado Condominial",
      location: "Brasília, DF",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Ricardo",
      text: "Recomendo aos meus clientes pela segurança jurídica. O registro de logs e a criptografia dos votos atendem perfeitamente às exigências da Lei 14.309/22.",
      rating: 5
    },
    {
      id: 6,
      name: "Fernanda Lima",
      role: "Presidente de Associação",
      location: "Florianópolis, SC",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Fernanda",
      text: "Gerir uma associação de bairro com 500 casas era impossível presencialmente. O Votzz trouxe democracia real para nossa comunidade. O suporte é excelente.",
      rating: 5
    },
    {
      id: 7,
      name: "Paulo Santos",
      role: "Proprietário Investidor",
      location: "Salvador, BA",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Paulo",
      text: "Tenho imóveis em várias cidades e era impossível estar presente. A ferramenta permitiu que eu exercesse meu direito de voto em todas as minhas unidades sem deslocamento.",
      rating: 5
    },
    {
      id: 8,
      name: "Juliana Ferreira",
      role: "Administradora de Bens",
      location: "Porto Alegre, RS",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Juliana",
      text: "A integração dos dados e a rapidez para configurar uma nova assembleia são os diferenciais. Em 10 minutos a pauta está no ar e os moradores notificados.",
      rating: 4
    },
    {
      id: 9,
      name: "Condomínio Blue Towers",
      role: "Equipe de Gestão",
      location: "Recife, PE",
      avatar: "https://api.dicebear.com/7.x/identicon/svg?seed=BlueTowers",
      text: "Utilizamos para eleição de síndico e aprovação de obras. O sistema de chat prévio ajudou a esvaziar as discussões acaloradas e focar na votação objetiva.",
      rating: 5
    }
  ];

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      <TestimonialsHeader />

      {/* Hero Section */}
      <section className="pt-32 pb-20 bg-slate-900 text-white text-center px-4 relative overflow-hidden">
        <div className="absolute inset-0 bg-emerald-600/10 opacity-30"></div>
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-500 rounded-full blur-[128px] opacity-10"></div>
        <div className="absolute bottom-0 left-0 w-72 h-72 bg-emerald-500 rounded-full blur-[100px] opacity-10"></div>
        
        <h1 className="text-4xl md:text-5xl font-extrabold mb-6 relative z-10">
          Histórias de Sucesso
        </h1>
        <p className="text-xl text-slate-300 max-w-2xl mx-auto relative z-10">
          Veja como síndicos, administradoras e moradores estão transformando a gestão de seus condomínios com o Votzz.
        </p>
      </section>

      {/* Testimonials Grid */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {reviews.map((review) => (
              <div key={review.id} className="bg-white p-8 rounded-2xl shadow-sm border border-slate-100 hover:shadow-xl transition-shadow duration-300 flex flex-col">
                <div className="flex items-center gap-1 text-yellow-400 mb-6">
                   {[...Array(5)].map((_, i) => (
                     <Star key={i} className={`w-5 h-5 ${i < review.rating ? 'fill-current' : 'text-slate-200'}`} />
                   ))}
                </div>
                
                <div className="relative mb-6 flex-1">
                   <Quote className="absolute -top-2 -left-2 w-8 h-8 text-emerald-100 transform -scale-x-100" />
                   <p className="text-slate-600 relative z-10 italic leading-relaxed">"{review.text}"</p>
                </div>

                <div className="flex items-center border-t border-slate-50 pt-6 mt-auto">
                   <div className="w-12 h-12 rounded-full overflow-hidden bg-slate-100 mr-4 border-2 border-white shadow-sm flex-shrink-0">
                      <img src={review.avatar} alt={review.name} className="w-full h-full object-cover" />
                   </div>
                   <div>
                      <h4 className="font-bold text-slate-900 text-sm">{review.name}</h4>
                      <p className="text-xs text-emerald-600 font-medium">{review.role}</p>
                      <p className="text-xs text-slate-400 flex items-center mt-0.5">
                        <Building2 className="w-3 h-3 mr-1" /> {review.location}
                      </p>
                   </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-emerald-50 border-y border-emerald-100">
         <div className="max-w-4xl mx-auto px-4 text-center">
            <h2 className="text-3xl font-bold text-emerald-900 mb-4">Faça parte dessas estatísticas</h2>
            <p className="text-emerald-700 mb-8 text-lg">Junte-se aos condomínios que modernizaram suas decisões.</p>
            <div className="flex flex-col sm:flex-row justify-center gap-4">
               <Link 
                 to="/login" 
                 state={{ isRegister: true }}
                 className="bg-emerald-600 text-white px-8 py-4 rounded-full font-bold shadow-lg hover:bg-emerald-700 transition-colors flex items-center justify-center"
               >
                 Começar Agora <ArrowRight className="ml-2 w-5 h-5" />
               </Link>
               <button className="bg-white text-emerald-700 border border-emerald-200 px-8 py-4 rounded-full font-bold hover:bg-emerald-50 transition-colors">
                 Falar com Consultor
               </button>
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
                    <li><Link to="/privacy" className="hover:text-white">Privacidade</Link></li>
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

export default Testimonials;

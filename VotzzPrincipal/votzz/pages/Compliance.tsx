
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  ShieldCheck, 
  Scale, 
  Lock, 
  FileText, 
  Database, 
  CheckCircle, 
  Menu, 
  X,
  Server,
  Eye,
  FileCheck
} from 'lucide-react';
import { Logo } from '../components/Logo';

const ComplianceHeader: React.FC = () => {
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
          <Link to="/compliance" className={`text-sm font-bold ${isScrolled ? 'text-emerald-600' : 'text-white'}`}>Compliance</Link>
          <Link to="/faq" className={`text-sm font-medium ${isScrolled ? 'text-slate-600 hover:text-emerald-600' : 'text-slate-300 hover:text-white'}`}>Ajuda</Link>
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
            <Link to="/compliance" className="py-2 font-bold text-emerald-600 border-b border-slate-50">Compliance</Link>
            <Link to="/pricing" className="py-2 border-b border-slate-50">Preços</Link>
            <Link to="/login" className="py-2 text-emerald-600">Entrar</Link>
          </div>
        )}
    </header>
  );
};

const Compliance: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      <ComplianceHeader />

      {/* Hero Section */}
      <section className="pt-32 pb-20 bg-slate-900 text-white relative overflow-hidden">
        <div className="absolute top-0 right-0 w-full h-full bg-[radial-gradient(circle_at_top_right,_var(--tw-gradient-stops))] from-slate-800 via-slate-900 to-slate-900 pointer-events-none"></div>
        <div className="absolute top-20 right-20 w-72 h-72 bg-blue-500 rounded-full blur-[128px] opacity-10"></div>
        
        <div className="max-w-7xl mx-auto px-4 text-center relative z-10">
          <div className="inline-flex items-center gap-2 bg-slate-800 border border-slate-700 text-slate-300 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide mb-8">
             <ShieldCheck className="w-4 h-4 text-emerald-500" /> Segurança Jurídica e Técnica
          </div>
          <h1 className="text-4xl md:text-5xl font-extrabold mb-6 leading-tight">
             Sua gestão protegida pela <br />
             <span className="text-emerald-400">Lei e pela Tecnologia</span>
          </h1>
          <p className="text-xl text-slate-400 max-w-3xl mx-auto mb-10 leading-relaxed">
             O Votzz foi desenvolvido em estrita conformidade com o Código Civil Brasileiro, a LGPD e os mais altos padrões de segurança da informação.
          </p>
        </div>
      </section>

      {/* Legal Pillars */}
      <section className="py-20 px-4 -mt-10 relative z-20">
         <div className="max-w-7xl mx-auto">
            <div className="grid md:grid-cols-3 gap-8">
               <div className="bg-white p-8 rounded-2xl shadow-xl border border-slate-200">
                  <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center mb-6">
                     <Scale className="w-6 h-6 text-blue-600" />
                  </div>
                  <h3 className="text-xl font-bold text-slate-900 mb-3">Lei 14.309/2022</h3>
                  <p className="text-slate-600 text-sm leading-relaxed">
                     Plataforma totalmente adaptada à lei que alterou o Código Civil (Art. 1.354-A), regulamentando as assembleias e votações eletrônicas em condomínios. Garantimos a identificação e a segurança do voto.
                  </p>
               </div>

               <div className="bg-white p-8 rounded-2xl shadow-xl border border-slate-200">
                  <div className="w-12 h-12 bg-emerald-100 rounded-xl flex items-center justify-center mb-6">
                     <Database className="w-6 h-6 text-emerald-600" />
                  </div>
                  <h3 className="text-xl font-bold text-slate-900 mb-3">LGPD Compliance</h3>
                  <p className="text-slate-600 text-sm leading-relaxed">
                     Respeitamos a Lei Geral de Proteção de Dados. Dados sensíveis são minimizados, o consentimento é explícito e você tem total controle sobre a exclusão e portabilidade das informações.
                  </p>
               </div>

               <div className="bg-white p-8 rounded-2xl shadow-xl border border-slate-200">
                  <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center mb-6">
                     <Lock className="w-6 h-6 text-purple-600" />
                  </div>
                  <h3 className="text-xl font-bold text-slate-900 mb-3">Criptografia Militar</h3>
                  <p className="text-slate-600 text-sm leading-relaxed">
                     Todos os votos recebem um hash criptográfico (SHA-256) único e imutável. Utilizamos SSL/TLS em todas as comunicações e bancos de dados criptografados em repouso.
                  </p>
               </div>
            </div>
         </div>
      </section>

      {/* Audit & Technical Details */}
      <section className="py-20 bg-white">
         <div className="max-w-7xl mx-auto px-4">
            <div className="flex flex-col lg:flex-row items-center gap-16">
               <div className="lg:w-1/2">
                  <h2 className="text-3xl font-bold text-slate-900 mb-6">Auditoria Completa e Rastreabilidade</h2>
                  <p className="text-slate-600 mb-8">
                     Em caso de impugnação ou dúvida, o Votzz fornece um dossiê técnico completo. Diferente de uma votação em papel ou formulário simples, nosso sistema registra "quem, quando e de onde" cada ação foi realizada, sem quebrar o sigilo do voto quando configurado como secreto.
                  </p>
                  
                  <ul className="space-y-4">
                     {[
                        "Log de IP e Timestamp para cada voto.",
                        "Assinatura digital do síndico na ata gerada.",
                        "Hash de integridade para garantir que votos não foram alterados no banco.",
                        "Controle de acesso baseado em funções (RBAC)."
                     ].map((item, i) => (
                        <li key={i} className="flex items-start">
                           <CheckCircle className="w-5 h-5 text-emerald-600 mr-3 flex-shrink-0 mt-0.5" />
                           <span className="text-slate-700 text-sm">{item}</span>
                        </li>
                     ))}
                  </ul>

                  <div className="mt-8 p-4 bg-slate-50 rounded-lg border border-slate-200">
                     <div className="flex items-center gap-2 text-xs font-mono text-slate-500 mb-2">
                        <Server className="w-4 h-4" /> Exemplo de Log de Auditoria
                     </div>
                     <div className="font-mono text-xs text-slate-600 bg-white p-3 rounded border border-slate-100 overflow-x-auto">
                        {`{
  "event": "VOTE_CAST",
  "user_id": "usr_8921...",
  "timestamp": "2023-10-25T14:32:01Z",
  "ip_hash": "a1b2c3d4...",
  "integrity_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}`}
                     </div>
                  </div>
               </div>
               
               <div className="lg:w-1/2 bg-slate-50 p-8 rounded-2xl border border-slate-100">
                  <h3 className="font-bold text-slate-900 mb-6">Transparência vs. Sigilo</h3>
                  <div className="space-y-6">
                     <div className="flex gap-4">
                        <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
                           <Eye className="w-5 h-5 text-blue-600" />
                        </div>
                        <div>
                           <h4 className="font-bold text-slate-800 text-sm">Voto Aberto</h4>
                           <p className="text-xs text-slate-500 mt-1">Quando configurado, o sistema exibe publicamente a escolha de cada unidade, conforme exigido em certas pautas do Código Civil. Ideal para obras e alterações de convenção.</p>
                        </div>
                     </div>

                     <div className="flex gap-4">
                        <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center flex-shrink-0">
                           <Lock className="w-5 h-5 text-purple-600" />
                        </div>
                        <div>
                           <h4 className="font-bold text-slate-800 text-sm">Voto Secreto</h4>
                           <p className="text-xs text-slate-500 mt-1">Para eleições de síndico (quando a convenção permite), o sistema desvincula a identidade da escolha no painel público, mantendo apenas o registro de que a unidade votou para evitar duplicidade.</p>
                        </div>
                     </div>

                     <div className="flex gap-4">
                        <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center flex-shrink-0">
                           <FileCheck className="w-5 h-5 text-emerald-600" />
                        </div>
                        <div>
                           <h4 className="font-bold text-slate-800 text-sm">Validação de Unidade</h4>
                           <p className="text-xs text-slate-500 mt-1">O sistema impede que inadimplentes votem (se configurado pelo administrador) e bloqueia múltiplos votos da mesma unidade, garantindo a lisura do pleito.</p>
                        </div>
                     </div>
                  </div>
               </div>
            </div>
         </div>
      </section>

      {/* FAQ Snippet */}
      <section className="py-20 bg-slate-900 text-white">
         <div className="max-w-4xl mx-auto px-4 text-center">
            <h2 className="text-3xl font-bold mb-8">Dúvidas Comuns sobre Legalidade</h2>
            <div className="grid md:grid-cols-2 gap-6 text-left">
               <div className="bg-slate-800 p-6 rounded-xl border border-slate-700">
                  <h4 className="font-bold text-emerald-400 mb-2">A ata digital tem validade?</h4>
                  <p className="text-sm text-slate-300">Sim. A ata gerada pelo sistema pode ser impressa, assinada pelo presidente da mesa e registrada em cartório, ou assinada digitalmente com certificado ICP-Brasil.</p>
               </div>
               <div className="bg-slate-800 p-6 rounded-xl border border-slate-700">
                  <h4 className="font-bold text-emerald-400 mb-2">Como garantem que sou eu?</h4>
                  <p className="text-sm text-slate-300">Utilizamos autenticação segura via e-mail e senha. Para maior segurança, o administrador pode exigir validação de dois fatores (2FA) via Google Authenticator.</p>
               </div>
            </div>
            
            <div className="mt-12">
               <Link to="/login" state={{ isRegister: true }} className="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-4 rounded-full font-bold transition-all shadow-lg inline-flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5" /> Criar Conta Segura
               </Link>
            </div>
         </div>
      </section>

      {/* Footer (Shared) */}
      <footer className="bg-slate-950 text-slate-400 py-12 border-t border-slate-800">
        <div className="max-w-7xl mx-auto px-4 text-center md:text-left grid md:grid-cols-4 gap-8">
            <div>
               <Logo theme="light" />
               <p className="text-sm mt-4">Tecnologia para decisões democráticas e transparentes.</p>
            </div>
            <div>
               <h4 className="text-white font-bold mb-4">Empresa</h4>
               <ul className="space-y-2 text-sm">
                  <li><Link to="/pricing" className="hover:text-white">Preços</Link></li>
                  <li><Link to="/blog" className="hover:text-white">Blog</Link></li>
                  <li><Link to="/faq" className="hover:text-white">FAQ</Link></li>
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
        <div className="max-w-7xl mx-auto px-4 mt-12 pt-8 border-t border-slate-800 text-center text-xs">
          &copy; {new Date().getFullYear()} Votzz. Todos os direitos reservados.
        </div>
      </footer>
    </div>
  );
};

export default Compliance;


import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Menu, X, FileText, ArrowLeft, Shield } from 'lucide-react';
import { Logo } from '../components/Logo';

const TermsHeader: React.FC = () => {
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

        <nav className="hidden md:flex items-center space-x-8">
          <Link to="/pricing" className={`text-sm font-medium ${isScrolled ? 'text-slate-600 hover:text-emerald-600' : 'text-slate-300 hover:text-white'}`}>Preços</Link>
          <Link to="/compliance" className={`text-sm font-medium ${isScrolled ? 'text-slate-600 hover:text-emerald-600' : 'text-slate-300 hover:text-white'}`}>Compliance</Link>
          <Link to="/login" className={`bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2 rounded-full text-sm font-bold transition-all shadow-md`}>
            Entrar
          </Link>
        </nav>

        <button className="md:hidden p-2" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
          {mobileMenuOpen ? <X className={isScrolled ? 'text-slate-800' : 'text-white'} /> : <Menu className={isScrolled ? 'text-slate-800' : 'text-white'} />}
        </button>
      </div>

       {mobileMenuOpen && (
          <div className="md:hidden absolute top-full left-0 right-0 bg-white border-t border-slate-100 shadow-lg p-4 flex flex-col space-y-4 animate-in slide-in-from-top-2 text-slate-800">
            <Link to="/pricing" className="py-2 border-b border-slate-50">Preços</Link>
            <Link to="/compliance" className="py-2 border-b border-slate-50">Compliance</Link>
            <Link to="/login" className="py-2 text-emerald-600 font-bold">Entrar</Link>
          </div>
        )}
    </header>
  );
};

const TermsOfUse: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      <TermsHeader />

      {/* Header Title */}
      <section className="pt-32 pb-12 bg-slate-900 text-white px-4">
        <div className="max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 bg-slate-800 border border-slate-700 text-slate-300 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide mb-6">
             <FileText className="w-4 h-4 text-emerald-500" /> Documento Legal
          </div>
          <h1 className="text-3xl md:text-4xl font-bold mb-4">Termos de Uso</h1>
          <p className="text-slate-400">Última atualização: 20/01/2026</p>
        </div>
      </section>

      {/* Content */}
      <section className="py-12 px-4">
        <div className="max-w-4xl mx-auto bg-white rounded-2xl shadow-sm border border-slate-200 p-8 md:p-12 prose prose-slate max-w-none">
          
          <div className="mb-8 p-4 bg-blue-50 border-l-4 border-blue-500 text-blue-900 text-sm rounded-r-lg">
            Bem-vindo ao Votzz. Estes Termos de Uso regulam o acesso e a utilização do aplicativo e de todas as suas funcionalidades, incluindo, mas não se limitando a: votação online, assembleias digitais, governança digital, comunicados institucionais e agendamento de áreas comuns, aplicáveis a condomínios, empresas, associações e clubes.
            <br/><br/>
            <strong>Ao acessar ou utilizar a plataforma, o USUÁRIO declara que leu, compreendeu e concorda integralmente com estes Termos.</strong>
          </div>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">1. DEFINIÇÕES</h3>
          <p>Para fins destes Termos:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li><strong>Plataforma:</strong> o aplicativo Votzz.</li>
            <li><strong>Administrador:</strong> síndico, gestor, representante legal ou pessoa autorizada.</li>
            <li><strong>Usuário:</strong> pessoa física com acesso à plataforma.</li>
            <li><strong>Instituição:</strong> condomínio, empresa, associação ou clube cadastrado.</li>
            <li><strong>Assembleia Digital:</strong> reunião realizada de forma virtual ou híbrida.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">2. OBJETO</h3>
          <p>A plataforma tem por objeto fornecer ferramentas tecnológicas de apoio à gestão institucional, incluindo:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li>Realização de assembleias e votações eletrônicas;</li>
            <li>Emissão de atas e relatórios;</li>
            <li>Governança digital com selo verificável;</li>
            <li>Gestão de comunicados oficiais;</li>
            <li>Agendamento e controle de uso de áreas comuns.</li>
          </ul>
          <p className="text-sm italic text-slate-500 mt-2">A plataforma não substitui obrigações legais, convenções internas ou assessoria jurídica especializada.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">3. BASE LEGAL</h3>
          <p>As funcionalidades de assembleia e votação eletrônica são oferecidas em conformidade com o <strong>art. 1.354-A do Código Civil Brasileiro</strong>, respeitando quóruns legais, convocação formal e registro de decisões.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">4. CADASTRO E RESPONSABILIDADES</h3>
          <h4 className="font-bold text-slate-800 mt-4">4.1. O Administrador é responsável por:</h4>
          <ul className="list-disc pl-5 space-y-2">
            <li>Cadastrar corretamente a instituição, unidades e usuários;</li>
            <li>Definir regras internas e permissões;</li>
            <li>Garantir que apenas pessoas com direito participem das votações.</li>
          </ul>
          <h4 className="font-bold text-slate-800 mt-4">4.2. O Usuário compromete-se a:</h4>
          <ul className="list-disc pl-5 space-y-2">
            <li>Fornecer informações verídicas;</li>
            <li>Manter a confidencialidade de suas credenciais;</li>
            <li>Utilizar a plataforma de forma ética e legal.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">5. VOTAÇÕES E ASSEMBLEIAS</h3>
          <ul className="list-decimal pl-5 space-y-2">
            <li>As votações são registradas com data e hora, vínculo à unidade ou matrícula, logs técnicos e trilha de auditoria.</li>
            <li>Após o encerramento da assembleia, os votos não poderão ser alterados.</li>
            <li>A validade das decisões depende também do cumprimento da convenção ou estatuto da instituição.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">6. GOVERNANÇA DIGITAL E SELO</h3>
          <ul className="list-decimal pl-5 space-y-2">
            <li>O Selo de Governança Digital indica que a instituição utiliza boas práticas de transparência e organização.</li>
            <li>O selo não substitui auditorias externas, não garante inexistência de litígios e pode ser suspenso em caso de descumprimento dos critérios técnicos.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">7. COMUNICADOS E AGENDAMENTOS</h3>
          <ul className="list-decimal pl-5 space-y-2">
            <li>Comunicados publicados na plataforma possuem caráter oficial.</li>
            <li>O agendamento de áreas comuns está sujeito às regras internas da instituição, pode exigir aprovação e/ou pagamento e gera histórico rastreável.</li>
            <li>A plataforma não se responsabiliza por danos decorrentes do uso indevido das áreas.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">8. PLANOS, PAGAMENTOS E CANCELAMENTO</h3>
          <ul className="list-decimal pl-5 space-y-2">
            <li>O acesso às funcionalidades varia conforme o plano contratado.</li>
            <li>Planos anuais podem incluir desconto, acesso ao selo de governança e funcionalidades avançadas.</li>
            <li>Cancelamentos não geram reembolso proporcional, salvo disposição contratual específica.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">9. LGPD E PRIVACIDADE</h3>
          <ul className="list-decimal pl-5 space-y-2">
            <li>Os dados pessoais são tratados conforme a Lei nº 13.709/2018 (LGPD).</li>
            <li>A plataforma adota medidas técnicas e administrativas para proteção dos dados.</li>
            <li>Dados sensíveis não são exibidos publicamente.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">10. LIMITAÇÃO DE RESPONSABILIDADE</h3>
          <p>A plataforma não se responsabiliza por:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li>Decisões tomadas pelos usuários;</li>
            <li>Uso indevido das informações;</li>
            <li>Falhas decorrentes de informações incorretas fornecidas pelos administradores;</li>
            <li>Conflitos internos entre membros da instituição.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">11. PROPRIEDADE INTELECTUAL</h3>
          <p>Todo o conteúdo, software, layout e marcas pertencem à Votzz, sendo vedada a cópia ou exploração sem autorização.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">12. ALTERAÇÕES DOS TERMOS</h3>
          <p>Os Termos poderão ser atualizados a qualquer tempo, com aviso prévio na plataforma.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">13. FORO</h3>
          <p>Fica eleito o foro da comarca de Goiânia, com renúncia a qualquer outro, por mais privilegiado que seja.</p>

          <div className="mt-12 pt-8 border-t border-slate-200">
             <Link to="/" className="text-emerald-600 font-bold hover:text-emerald-700 flex items-center">
                <ArrowLeft className="w-4 h-4 mr-2" /> Voltar para a página inicial
             </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
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
        <div className="max-w-7xl mx-auto px-4 mt-12 pt-8 border-t border-slate-800 text-center text-xs">
          &copy; {new Date().getFullYear()} Votzz. Todos os direitos reservados.
        </div>
      </footer>
    </div>
  );
};

export default TermsOfUse;

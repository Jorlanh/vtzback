
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Menu, X, Shield, Lock, ArrowLeft, Eye } from 'lucide-react';
import { Logo } from '../components/Logo';

const PrivacyHeader: React.FC = () => {
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

const PrivacyPolicy: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      <PrivacyHeader />

      {/* Hero Section */}
      <section className="pt-32 pb-12 bg-slate-900 text-white px-4">
        <div className="max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 bg-slate-800 border border-slate-700 text-slate-300 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide mb-6">
             <Lock className="w-4 h-4 text-emerald-500" /> Proteção de Dados
          </div>
          <h1 className="text-3xl md:text-4xl font-bold mb-4">Política de Privacidade</h1>
          <p className="text-slate-400">Última atualização: 20/01/2026</p>
        </div>
      </section>

      {/* Content */}
      <section className="py-12 px-4">
        <div className="max-w-4xl mx-auto bg-white rounded-2xl shadow-sm border border-slate-200 p-8 md:p-12 prose prose-slate max-w-none">
          
          <div className="mb-8 p-4 bg-emerald-50 border-l-4 border-emerald-500 text-emerald-900 text-sm rounded-r-lg">
            A presente Política de Privacidade descreve como o Votzz coleta, utiliza, armazena, compartilha e protege os dados pessoais dos usuários, em conformidade com a Lei nº 13.709/2018 – Lei Geral de Proteção de Dados Pessoais (LGPD).
            <br/><br/>
            <strong>Ao utilizar a plataforma, o USUÁRIO declara ciência e concordância com esta Política.</strong>
          </div>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">1. QUEM SOMOS</h3>
          <p>O Votzz é uma plataforma SaaS destinada à votação online, assembleias digitais, governança digital, gestão de comunicados e agendamento de áreas comuns, utilizada por condomínios, empresas, associações e clubes.</p>
          <p>Para fins da LGPD, atuamos como <strong>Operador de Dados</strong>, enquanto a instituição contratante atua como <strong>Controladora de Dados</strong>.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">2. DADOS COLETADOS</h3>
          <p>A plataforma pode coletar os seguintes dados pessoais:</p>
          
          <h4 className="font-bold text-slate-800 mt-4 text-sm uppercase">2.1 Dados cadastrais</h4>
          <ul className="list-disc pl-5 space-y-1">
            <li>Nome completo</li>
            <li>E-mail</li>
            <li>Telefone</li>
            <li>Unidade, matrícula ou vínculo institucional</li>
          </ul>

          <h4 className="font-bold text-slate-800 mt-4 text-sm uppercase">2.2 Dados de autenticação e uso</h4>
          <ul className="list-disc pl-5 space-y-1">
            <li>Login e senha (criptografados)</li>
            <li>Endereço IP</li>
            <li>Data e hora de acesso</li>
            <li>Logs de ações (votos, presença, reservas, confirmações)</li>
          </ul>

          <h4 className="font-bold text-slate-800 mt-4 text-sm uppercase">2.3 Dados institucionais</h4>
          <ul className="list-disc pl-5 space-y-1">
            <li>Cargo ou função</li>
            <li>Permissões de acesso</li>
          </ul>
          
          <p className="mt-4 text-sm bg-slate-100 p-3 rounded border border-slate-200">📌 A plataforma não coleta dados sensíveis, como origem racial, religião, opinião política ou dados de saúde.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">3. FINALIDADE DO TRATAMENTO</h3>
          <p>Os dados são tratados para:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li>Permitir acesso seguro à plataforma;</li>
            <li>Viabilizar votações e assembleias;</li>
            <li>Gerar atas, relatórios e trilhas de auditoria;</li>
            <li>Gerenciar comunicados oficiais;</li>
            <li>Controlar reservas de áreas comuns;</li>
            <li>Cumprir obrigações legais e regulatórias;</li>
            <li>Garantir segurança, prevenção à fraude e integridade dos registros.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">4. BASE LEGAL</h3>
          <p>O tratamento dos dados pessoais ocorre com base em:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li>Execução de contrato;</li>
            <li>Cumprimento de obrigação legal;</li>
            <li>Legítimo interesse;</li>
            <li>Consentimento, quando aplicável.</li>
          </ul>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">5. COMPARTILHAMENTO DE DADOS</h3>
          <p>Os dados pessoais poderão ser compartilhados:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li>Com a instituição contratante;</li>
            <li>Com fornecedores de tecnologia e hospedagem;</li>
            <li>Para cumprimento de ordem judicial ou obrigação legal.</li>
          </ul>
          <p className="font-bold text-slate-900 mt-2">Não realizamos venda de dados pessoais.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">6. ARMAZENAMENTO E SEGURANÇA</h3>
          <p>A plataforma adota medidas técnicas e administrativas para proteção dos dados, incluindo:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li>Criptografia;</li>
            <li>Controle de acesso por perfil;</li>
            <li>Logs de auditoria;</li>
            <li>Armazenamento em ambiente seguro.</li>
          </ul>
          <p>Os dados são armazenados pelo período necessário ao cumprimento das finalidades legais e contratuais.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">7. DIREITOS DO TITULAR</h3>
          <p>Nos termos da LGPD, o usuário pode solicitar:</p>
          <ul className="list-disc pl-5 space-y-2">
            <li>Confirmação da existência de tratamento;</li>
            <li>Acesso aos dados;</li>
            <li>Correção de dados incompletos ou desatualizados;</li>
            <li>Anonimização, bloqueio ou eliminação;</li>
            <li>Portabilidade;</li>
            <li>Revogação do consentimento.</li>
          </ul>
          <p className="text-sm italic text-slate-500 mt-2">As solicitações devem ser feitas por meio do canal indicado pela instituição controladora.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">8. GOVERNANÇA DIGITAL E TRANSPARÊNCIA</h3>
          <p>Dados exibidos em links públicos de governança digital são limitados a informações institucionais e agregadas, não sendo divulgados dados pessoais ou votos individualizados.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">9. COOKIES E TECNOLOGIAS SEMELHANTES</h3>
          <p>A plataforma pode utilizar cookies estritamente necessários para funcionamento, autenticação e segurança.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">10. ALTERAÇÕES DA POLÍTICA</h3>
          <p>Esta Política poderá ser atualizada a qualquer tempo. Alterações relevantes serão comunicadas aos usuários.</p>

          <h3 className="text-xl font-bold text-slate-900 mt-8 mb-4">11. CONTATO</h3>
          <p>Em caso de dúvidas sobre esta Política de Privacidade, entre em contato pelo canal oficial da plataforma.</p>

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

export default PrivacyPolicy;

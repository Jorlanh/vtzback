import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, Key, HelpCircle, ShieldAlert, Phone } from 'lucide-react';
import { api } from '../services/api';
import { Logo } from '../components/Logo';

export default function ForgotPassword() {
  // Estados: 'CHOICE', 'EMAIL_FORM', 'TOKEN_FORM', 'FORGOT_EMAIL_INFO'
  const [view, setView] = useState<'CHOICE' | 'EMAIL_FORM' | 'TOKEN_FORM' | 'FORGOT_EMAIL_INFO'>('CHOICE');
  
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRequestToken = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      // Endpoint que já configuramos no AuthController
      await api.post('/auth/forgot-password', { email });
      setView('TOKEN_FORM');
      alert('Código de recuperação enviado para seu e-mail!');
    } catch (err) {
      alert('E-mail não encontrado na base de dados.');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.post('/auth/reset-password', { code: token, newPassword });
      alert('Senha alterada com sucesso! Você já pode fazer login.');
      window.location.href = '/login'; // Redireciona para login
    } catch (err) {
      alert('Código inválido ou expirado. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center p-4">
      <div className="mb-8"><Link to="/"><Logo theme="dark" /></Link></div>
      
      <div className="bg-slate-800 p-8 rounded-2xl shadow-xl w-full max-w-md border border-slate-700">
        
        {/* TELA 1: ESCOLHA */}
        {view === 'CHOICE' && (
          <div className="space-y-6 animate-in fade-in">
            <div className="text-center">
              <h2 className="text-2xl font-bold text-white mb-2">Problemas de Acesso?</h2>
              <p className="text-slate-400 text-sm">Selecione o que você precisa recuperar.</p>
            </div>

            <button 
              onClick={() => setView('EMAIL_FORM')}
              className="w-full bg-emerald-600/10 hover:bg-emerald-600/20 border border-emerald-500/50 p-4 rounded-xl flex items-center gap-4 group transition-all"
            >
              <div className="bg-emerald-500 p-3 rounded-full text-white group-hover:scale-110 transition-transform">
                <Key size={20} />
              </div>
              <div className="text-left">
                <h3 className="font-bold text-white">Esqueci minha Senha</h3>
                <p className="text-xs text-slate-400">Tenho acesso ao meu e-mail cadastrado.</p>
              </div>
            </button>

            <button 
              onClick={() => setView('FORGOT_EMAIL_INFO')}
              className="w-full bg-slate-700/30 hover:bg-slate-700/50 border border-slate-600 p-4 rounded-xl flex items-center gap-4 group transition-all"
            >
              <div className="bg-slate-600 p-3 rounded-full text-white group-hover:scale-110 transition-transform">
                <HelpCircle size={20} />
              </div>
              <div className="text-left">
                <h3 className="font-bold text-white">Esqueci meu E-mail</h3>
                <p className="text-xs text-slate-400">Não lembro qual e-mail usei.</p>
              </div>
            </button>
          </div>
        )}

        {/* TELA 2: DIGITAR EMAIL (Para senha) */}
        {view === 'EMAIL_FORM' && (
          <form onSubmit={handleRequestToken} className="space-y-4 animate-in slide-in-from-right">
            <div className="text-center mb-6">
              <h2 className="text-xl font-bold text-white">Recuperar Senha</h2>
              <p className="text-slate-400 text-sm">Enviaremos um código para você.</p>
            </div>
            
            <div>
              <label className="text-slate-400 text-xs uppercase font-bold ml-1">E-mail Cadastrado</label>
              <div className="relative mt-1">
                <Mail className="absolute left-3 top-3 text-slate-500 w-5 h-5"/>
                <input 
                  required 
                  type="email" 
                  value={email} 
                  onChange={e => setEmail(e.target.value)} 
                  className="w-full bg-slate-900 text-white pl-10 p-3 rounded-lg border border-slate-600 focus:border-emerald-500 outline-none" 
                  placeholder="seu@email.com"
                />
              </div>
            </div>
            <button type="submit" disabled={loading} className="w-full bg-emerald-600 text-white py-3 rounded-lg font-bold hover:bg-emerald-500 transition-colors">
              {loading ? 'Enviando...' : 'Enviar Código'}
            </button>
            <button type="button" onClick={() => setView('CHOICE')} className="w-full text-slate-400 text-sm hover:text-white mt-2">Voltar</button>
          </form>
        )}

        {/* TELA 3: TROCAR SENHA (Token) */}
        {view === 'TOKEN_FORM' && (
          <form onSubmit={handleReset} className="space-y-4 animate-in slide-in-from-right">
            <div className="text-center mb-6">
              <h2 className="text-xl font-bold text-white">Criar Nova Senha</h2>
              <p className="text-emerald-400 text-sm">Código enviado para {email}</p>
            </div>

            <div>
              <label className="text-slate-400 text-xs uppercase font-bold ml-1">Código Recebido</label>
              <input 
                required 
                value={token} 
                onChange={e => setToken(e.target.value)} 
                className="w-full bg-slate-900 text-white p-3 rounded-lg border border-slate-600 outline-none text-center tracking-[0.5em] text-xl font-mono" 
                placeholder="000000"
              />
            </div>
            <div>
              <label className="text-slate-400 text-xs uppercase font-bold ml-1">Nova Senha</label>
              <div className="relative mt-1">
                <Key className="absolute left-3 top-3 text-slate-500 w-5 h-5"/>
                <input 
                  required 
                  type="password" 
                  value={newPassword} 
                  onChange={e => setNewPassword(e.target.value)} 
                  className="w-full bg-slate-900 text-white pl-10 p-3 rounded-lg border border-slate-600 focus:border-emerald-500 outline-none"
                  placeholder="Nova senha segura"
                />
              </div>
            </div>
            <button type="submit" disabled={loading} className="w-full bg-emerald-600 text-white py-3 rounded-lg font-bold hover:bg-emerald-500 transition-colors">
              {loading ? 'Salvando...' : 'Redefinir Senha'}
            </button>
          </form>
        )}

        {/* TELA 4: INFO SOBRE EMAIL ESQUECIDO */}
        {view === 'FORGOT_EMAIL_INFO' && (
          <div className="space-y-6 animate-in slide-in-from-right text-center">
            <div className="bg-amber-500/10 p-4 rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4">
              <ShieldAlert className="text-amber-500 w-8 h-8" />
            </div>
            
            <h3 className="text-xl font-bold text-white">Segurança de Dados</h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              Por motivos de segurança e proteção de dados (LGPD), não podemos revelar seu e-mail automaticamente.
            </p>

            <div className="bg-slate-700/50 p-4 rounded-xl text-left border border-slate-600">
              <h4 className="font-bold text-white mb-2 text-sm flex items-center gap-2"><Phone size={16}/> O que fazer?</h4>
              <ul className="text-sm text-slate-300 space-y-2 list-disc pl-4">
                <li><strong>Moradores:</strong> Procure a administração do condomínio. Eles podem ver e ajustar seu e-mail no painel de gestão.</li>
                <li><strong>Afiliados:</strong> Entre em contato com o suporte da Votzz informando seu CPF.</li>
              </ul>
            </div>

            <button type="button" onClick={() => setView('CHOICE')} className="w-full bg-slate-700 text-white py-3 rounded-lg font-bold hover:bg-slate-600 transition-colors">
              Entendi, voltar
            </button>
          </div>
        )}

        {/* Footer Comum */}
        <div className="mt-8 pt-6 border-t border-slate-700 text-center">
          <Link to="/login" className="text-slate-500 hover:text-white text-sm flex items-center justify-center gap-2 transition-colors">
            <ArrowLeft size={16}/> Voltar para Login
          </Link>
        </div>

      </div>
    </div>
  );
}
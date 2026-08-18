import React, { useState, useEffect } from 'react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { Lock, Mail, ShieldCheck, ArrowRight, AlertCircle, User as UserIcon, TrendingUp, Building, Phone, CheckCircle, Fingerprint, Plus, Trash2, Key, FileText, Eye, EyeOff } from 'lucide-react';
import { Logo } from '../components/Logo';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { ProfileOption } from '../types';

// Função para gerar o ID do dispositivo (ou recuperar)
const getDeviceId = () => {
    let id = localStorage.getItem('votzz_device_id');
    if (!id) {
        id = Date.now().toString(36) + Math.random().toString(36).substring(2);
        localStorage.setItem('votzz_device_id', id);
    }
    return id;
};

// --- CONTENT CONSTANTS (Termos e Política) ---
const TERMS_CONTENT = `
  <h3 class="font-bold text-lg mb-2">1. DEFINIÇÕES</h3>
  <p>Para fins destes Termos:</p>
  <ul class="list-disc pl-5 space-y-1 mb-4">
    <li><strong>Plataforma:</strong> o aplicativo Votzz.</li>
    <li><strong>Administrador:</strong> síndico, gestor, representante legal ou pessoa autorizada.</li>
    <li><strong>Usuário:</strong> pessoa física com acesso à plataforma.</li>
    <li><strong>Instituição:</strong> condomínio, empresa, associação ou clube cadastrado.</li>
  </ul>

  <h3 class="font-bold text-lg mb-2">2. OBJETO</h3>
  <p>A plataforma tem por objeto fornecer ferramentas tecnológicas de apoio à gestão institucional, incluindo:</p>
  <ul class="list-disc pl-5 space-y-1 mb-4">
    <li>Realização de assembleias e votações eletrônicas;</li>
    <li>Emissão de atas e relatórios;</li>
    <li>Governança digital com selo verificável;</li>
  </ul>

  <h3 class="font-bold text-lg mb-2">3. BASE LEGAL</h3>
  <p class="mb-4">As funcionalidades de assembleia e votação eletrônica são oferecidas em conformidade com o <strong>art. 1.354-A do Código Civil Brasileiro</strong>.</p>

  <h3 class="font-bold text-lg mb-2">4. CADASTRO E RESPONSABILIDADES</h3>
  <p class="mb-4">O Usuário compromete-se a fornecer informações verídicas e manter a confidencialidade de suas credenciais.</p>
`;

const PRIVACY_CONTENT = `
  <h3 class="font-bold text-lg mb-2">1. DADOS COLETADOS</h3>
  <p>A plataforma pode coletar os seguintes dados pessoais:</p>
  <ul class="list-disc pl-5 space-y-1 mb-4">
    <li>Dados cadastrais: Nome, E-mail, Telefone, Unidade.</li>
    <li>Dados de autenticação: Login, IP, Logs de ações.</li>
  </ul>
  
  <h3 class="font-bold text-lg mb-2">2. FINALIDADE</h3>
  <p class="mb-4">Os dados são tratados para permitir acesso seguro, viabilizar votações, gerar atas e cumprir obrigações legais.</p>

  <h3 class="font-bold text-lg mb-2">3. COMPARTILHAMENTO</h3>
  <p class="mb-4">Os dados podem ser compartilhados com a instituição contratante e fornecedores de tecnologia. Não vendemos dados.</p>

  <h3 class="font-bold text-lg mb-2">4. SEGURANÇA</h3>
  <p class="mb-4">Adotamos criptografia e controle de acesso para proteger seus dados.</p>
`;

// --- MODAL COMPONENT ---
const LegalModal: React.FC<{ title: string; content: string; onClose: () => void }> = ({ title, content, onClose }) => (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in duration-200">
        <div className="bg-white w-full max-w-2xl max-h-[80vh] rounded-2xl shadow-2xl flex flex-col overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50">
                <h3 className="font-bold text-xl text-slate-800 flex items-center gap-2">
                    <ShieldCheck className="text-emerald-500" /> {title}
                </h3>
                <button onClick={onClose} className="text-slate-400 hover:text-slate-600 transition-colors text-2xl font-light">&times;</button>
            </div>
            <div className="p-8 overflow-y-auto prose prose-slate max-w-none text-sm text-slate-600 leading-relaxed">
                <div dangerouslySetInnerHTML={{ __html: content }} />
            </div>
            <div className="p-4 border-t border-slate-100 bg-slate-50 flex justify-end">
                <button onClick={onClose} className="bg-emerald-600 text-white px-6 py-2 rounded-lg font-bold hover:bg-emerald-700 transition-colors">
                    Entendi e Concordo
                </button>
            </div>
        </div>
    </div>
);

const Auth: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { login } = useAuth();
  
  const isAffiliateContext = location.pathname.includes('/affiliate');

  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Login States
  const [loginInput, setLoginInput] = useState(''); 
  const [password, setPassword] = useState('');
  const [keepLogged, setKeepLogged] = useState(false); 
  
  // FIX: Estado para guardar o ID do perfil caso o 2FA seja solicitado após o "Leque"
  const [selectedProfileId, setSelectedProfileId] = useState<string>('');

  // Password Visibility States
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // 2FA STATE
  const [show2FAInput, setShow2FAInput] = useState(false);
  const [code2FA, setCode2FA] = useState('');
  const [trustDevice, setTrustDevice] = useState(false); 

  // Multi-Perfil
  const [showProfileSelector, setShowProfileSelector] = useState(false);
  const [availableProfiles, setAvailableProfiles] = useState<ProfileOption[]>([]);

  // Cadastro Residente
  const [email, setEmail] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [nome, setNome] = useState('');
  const [cpf, setCpf] = useState('');
  const [whatsapp, setWhatsapp] = useState('');
  const [condoIdentifier, setCondoIdentifier] = useState('');
  const [secretKeyword, setSecretKeyword] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false); 

  // Multi-Unidades
  const [hasMultipleUnits, setHasMultipleUnits] = useState(false);
  const [unitsList, setUnitsList] = useState([{ bloco: '', unidade: '' }]);

  // --- MODAL STATES ---
  const [showTermsModal, setShowTermsModal] = useState(false);
  const [showPrivacyModal, setShowPrivacyModal] = useState(false);

  useEffect(() => {
    if (location.state?.isRegister) {
      setIsLogin(false);
    }
  }, [location.state]);

  const handleToggleMode = () => {
    setError('');
    setShow2FAInput(false);
    setCode2FA('');
    setTrustDevice(false);
    setSelectedProfileId(''); // Limpa seleção ao trocar modo
    if (isLogin) {
      if (isAffiliateContext) {
        navigate('/affiliate/register');
      } else {
        setIsLogin(false);
      }
    } else {
      setIsLogin(true);
    }
  };

  const handleUnitChange = (index: number, field: 'bloco' | 'unidade', value: string) => {
    const newUnits = [...unitsList];
    newUnits[index][field] = value;
    setUnitsList(newUnits);
  };

  const addUnitField = () => {
    setUnitsList([...unitsList, { bloco: '', unidade: '' }]);
  };

  const removeUnitField = (index: number) => {
    if (unitsList.length > 1) {
      const newUnits = unitsList.filter((_, i) => i !== index);
      setUnitsList(newUnits);
    }
  };

  const validateEmailDomain = (email: string) => {
    const allowedDomains = ['gmail.com', 'outlook.com', 'hotmail.com', 'yahoo.com', 'icloud.com', 'me.com', 'codecloudcorp.com'];
    const parts = email.split('@');
    if (parts.length < 2) return false;
    const domain = parts[1].toLowerCase().trim();
    return allowedDomains.includes(domain);
  };

  const executeLogin = async (profileIdParam?: string) => {
    setLoading(true);
    setError('');
    
    // FIX: Usa o profileId passado (clique) ou o estado salvo (fluxo 2FA)
    const effectiveProfileId = profileIdParam || selectedProfileId;

    try {
        const payload: any = { 
            login: loginInput, 
            password,
            selectedProfileId: effectiveProfileId,
            deviceId: getDeviceId(),
            keepLogged: keepLogged 
        };

        if (show2FAInput && code2FA) {
            // Garante que é número e remove espaços
            payload.code2fa = parseInt(code2FA.replace(/\D/g, ''));
            payload.trustDevice = trustDevice;
        }

        const response = await api.post('/auth/login', payload);
        const data = response.data;

        // Se o backend pedir 2FA
        if (data.requiresTwoFactor) {
            setShow2FAInput(true);
            setLoading(false);
            
            // Se o usuário tinha clicado num perfil específico, salvamos esse ID
            // para reenviar quando ele digitar o código
            if (profileIdParam) {
                setSelectedProfileId(profileIdParam);
                setShowProfileSelector(false); // Fecha o leque para mostrar o input de código
            }
            return;
        }

        // --- LÓGICA DO LEQUE ---
        // Se o backend retornar multipleProfiles e ainda não selecionamos um
        if (data.multipleProfiles && !effectiveProfileId) {
            setAvailableProfiles(data.profiles || []);
            setShowProfileSelector(true);
            setLoading(false);
            return;
        }

        login(data);
        
        switch (data.role) {
          case 'ADMIN': navigate('/admin/dashboard'); break;
          case 'AFILIADO': navigate('/affiliate/dashboard'); break;
          default: navigate('/dashboard');
        }
    } catch (err: any) {
        console.error(err);
        const msg = err.response?.data?.message || err.response?.data || err.message || 'Erro na operação.';
        
        if (show2FAInput) {
             setError("Código incorreto ou expirado.");
        } else {
             setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
        }
        setLoading(false);
    }
  };

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isLogin) {
        // Chama executeLogin. Se estivermos na tela de 2FA, ele usará o selectedProfileId do state
        await executeLogin();
    } else {
        if (!acceptedTerms) {
            setError('Você precisa aceitar os termos de uso.');
            return;
        }

        setLoading(true);
        try {
            if (password !== confirmPassword) {
              setError('As senhas não coincidem.');
              setLoading(false);
              return;
            }

            if (!validateEmailDomain(email)) {
                setError('Provedor de e-mail não aceito. Utilize: Gmail, Outlook, Hotmail, Yahoo, iCloud ou Me.');
                setLoading(false);
                return;
            }

            const invalidUnit = unitsList.find(u => !u.unidade.trim() || !u.bloco.trim());
            if (invalidUnit) {
                setError('Preencha Bloco e Unidade para todos os apartamentos adicionados.');
                setLoading(false);
                return;
            }

            await api.post('/auth/register-resident', { 
              nome, 
              email, 
              password, 
              cpf, 
              whatsapp,
              condoIdentifier,
              secretKeyword,
              units: unitsList 
            });
            
            alert('Cadastro realizado com sucesso! Faça login para acessar suas unidades.');
            setIsLogin(true);
            setPassword('');
            setConfirmPassword('');
            setUnitsList([{ bloco: '', unidade: '' }]); 
        } catch (err: any) {
            console.error(err);
            const msg = err.response?.data?.message || err.response?.data || err.message || 'Erro no cadastro.';
            setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
        } finally {
            setLoading(false);
        }
    }
  };

  if (showProfileSelector) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-slate-900 p-4">
              <div className="bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md border border-slate-700 p-8 animate-in zoom-in-95 duration-300">
                <div className="text-center mb-6">
                    <Logo theme="dark" size="lg" />
                    <h2 className="text-white font-bold text-xl mt-4">Bem-vindo(a)!</h2>
                    <p className="text-slate-400 text-sm mt-1">Como você deseja acessar hoje?</p>
                </div>
                
                <div className="space-y-3 max-h-[60vh] overflow-y-auto pr-1 custom-scrollbar">
                    {availableProfiles.map((profile) => (
                        <button
                            key={profile.userId}
                            onClick={() => executeLogin(profile.userId)}
                            className="w-full bg-slate-700 hover:bg-slate-600 border border-slate-600 p-4 rounded-xl flex items-center justify-between group transition-all"
                        >
                            <div className="text-left">
                                <h3 className="font-bold text-white group-hover:text-emerald-400 transition-colors flex items-center gap-2">
                                    {profile.role === 'AFILIADO' ? <TrendingUp size={16}/> : <Building size={16}/>}
                                    {profile.contextName || 'Perfil Global'}
                                </h3>
                                
                                <div className="flex items-center gap-2 mt-2">
                                    <span className={`text-[10px] uppercase font-bold tracking-wide px-2 py-1 rounded ${
                                        profile.role === 'ADMIN' ? 'bg-red-500/20 text-red-400' :
                                        profile.role === 'SINDICO' ? 'bg-purple-500/20 text-purple-400' :
                                        profile.role === 'AFILIADO' ? 'bg-blue-500/20 text-blue-400' :
                                        'bg-emerald-500/20 text-emerald-400'
                                    }`}>
                                        {profile.role === 'ADM_CONDO' ? 'ADMIN CONDO' : profile.role}
                                    </span>
                                    <span className="text-xs text-slate-400">{profile.userName}</span>
                                </div>
                            </div>
                            <ArrowRight className="text-slate-500 group-hover:text-emerald-400 group-hover:translate-x-1 transition-all" />
                        </button>
                    ))}
                </div>

                <button 
                    onClick={() => { setShowProfileSelector(false); setLoginInput(''); setPassword(''); }}
                    className="w-full mt-6 text-slate-400 hover:text-white text-sm underline"
                >
                    Voltar e usar outro login
                </button>
             </div>
             
             <style>{`
                .custom-scrollbar::-webkit-scrollbar { width: 6px; }
                .custom-scrollbar::-webkit-scrollbar-track { background: #1e293b; }
                .custom-scrollbar::-webkit-scrollbar-thumb { background: #475569; border-radius: 4px; }
                .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #64748b; }
             `}</style>
        </div>
      );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-900 p-4 relative overflow-hidden">
      
      {/* --- MODAIS RENDERIZADOS AQUI --- */}
      {showTermsModal && <LegalModal title="Termos de Uso" content={TERMS_CONTENT} onClose={() => setShowTermsModal(false)} />}
      {showPrivacyModal && <LegalModal title="Política de Privacidade" content={PRIVACY_CONTENT} onClose={() => setShowPrivacyModal(false)} />}

      {isAffiliateContext && (
        <div className="absolute top-0 right-0 p-10 opacity-5 pointer-events-none">
            <TrendingUp className="w-96 h-96 text-emerald-500" />
        </div>
      )}

      {!isAffiliateContext && isLogin && (
        <div className="absolute -bottom-20 -left-20 p-10 opacity-5 pointer-events-none">
            <Fingerprint className="w-96 h-96 text-emerald-500" />
        </div>
      )}

      <div className={`bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden border ${isAffiliateContext ? 'border-emerald-500/30' : 'border-slate-700'} relative z-10`}>
        
        <div className="bg-slate-900 p-8 flex flex-col items-center border-b border-slate-700">
          <Link to="/"><Logo theme="dark" size="lg" /></Link>
          <div className="mt-4 text-center">
            {isAffiliateContext ? (
              <span className="inline-block bg-emerald-500/10 text-emerald-400 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wide mb-2">Área do Parceiro</span>
            ) : (
              !isLogin && <span className="text-slate-500 text-sm block mb-1">Cadastro de Morador</span>
            )}
            <p className="text-slate-400 text-sm">
              {isLogin 
                ? (show2FAInput ? 'Verificação de Segurança' : 'Bem-vindo de volta! Acesse sua conta.') 
                : 'Preencha os dados para solicitar acesso.'}
            </p>
          </div>
        </div>

        <div className="p-8">
          {error && (
            <div className="bg-red-500/10 border border-red-500/50 text-red-200 p-3 rounded-lg flex items-center gap-2 mb-6 text-sm animate-pulse">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleAuth} className="space-y-5">
            
            {!isLogin && (
              <div className="space-y-4 animate-in slide-in-from-top-2 duration-300">
                
                <div className="relative">
                  <Building className="absolute left-3 top-3 h-5 w-5 text-emerald-500" />
                  <input 
                    type="text" 
                    placeholder="Nome do Condomínio ou CNPJ" 
                    value={condoIdentifier} 
                    onChange={e => setCondoIdentifier(e.target.value)} 
                    className="w-full bg-slate-900 text-white pl-10 p-3 border border-emerald-500/30 rounded-lg focus:border-emerald-500 outline-none transition-all placeholder:text-slate-500" 
                    required 
                  />
                  <p className="text-[10px] text-slate-500 mt-1 ml-1">Digite exatamente como consta no registro</p>
                </div>

                <div className="relative">
                  <UserIcon className="absolute left-3 top-3 h-5 w-5 text-slate-500" />
                  <input type="text" placeholder="Nome Completo" value={nome} onChange={e => setNome(e.target.value)} className="w-full bg-slate-900 text-white pl-10 p-3 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none" required />
                </div>

                <div className="relative">
                    <Mail className="absolute left-3 top-3 h-5 w-5 text-slate-500" />
                    <input type="email" value={email} placeholder="Seu melhor E-mail" onChange={e => setEmail(e.target.value)} className="w-full bg-slate-900 text-white pl-10 p-3 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none" required />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <input type="text" placeholder="CPF" value={cpf} onChange={e => setCpf(e.target.value)} className="bg-slate-900 text-white p-3 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none" required />
                  <div className="relative">
                    <Phone className="absolute left-3 top-3 h-5 w-5 text-slate-500" />
                    <input type="text" placeholder="WhatsApp" value={whatsapp} onChange={e => setWhatsapp(e.target.value)} className="w-full bg-slate-900 text-white pl-10 p-3 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none" required />
                  </div>
                </div>

                <div className="bg-slate-700/30 p-4 rounded-xl border border-slate-600 space-y-3">
                    <div className="flex justify-between items-center mb-2">
                        <span className="text-xs font-bold text-slate-400 uppercase">Suas Unidades</span>
                        <div className="flex items-center gap-2">
                            <input 
                                type="checkbox" 
                                id="multipleUnits" 
                                checked={hasMultipleUnits} 
                                onChange={e => setHasMultipleUnits(e.target.checked)}
                                className="w-4 h-4 rounded border-slate-500 bg-slate-800 text-emerald-500 focus:ring-emerald-500"
                            />
                            <label htmlFor="multipleUnits" className="text-xs text-white cursor-pointer select-none">
                                Tenho mais de uma
                            </label>
                        </div>
                    </div>

                    {unitsList.map((item, idx) => (
                        <div key={idx} className="flex gap-2 items-center animate-in fade-in slide-in-from-left-4">
                            <input 
                                type="text" 
                                placeholder="Bloco / Torre" 
                                value={item.bloco} 
                                onChange={e => handleUnitChange(idx, 'bloco', e.target.value)} 
                                className="bg-slate-900 text-white p-3 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none w-1/3 text-sm" 
                                required 
                            />
                            <input 
                                type="text" 
                                placeholder="Unidade (Ex: 101)" 
                                value={item.unidade} 
                                onChange={e => handleUnitChange(idx, 'unidade', e.target.value)} 
                                className="bg-slate-900 text-white p-3 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none flex-1 text-sm" 
                                required 
                            />
                            {hasMultipleUnits && unitsList.length > 1 && (
                                <button type="button" onClick={() => removeUnitField(idx)} className="text-red-400 hover:text-red-300 p-2">
                                    <Trash2 size={18} />
                                </button>
                            )}
                        </div>
                    ))}

                    {hasMultipleUnits && (
                        <button type="button" onClick={addUnitField} className="text-xs text-emerald-400 hover:text-emerald-300 flex items-center gap-1 font-bold mt-2">
                            <Plus size={14}/> Adicionar outra unidade
                        </button>
                    )}
                </div>
              </div>
            )}

            {isLogin && (
                <div className="space-y-4">
                    {!show2FAInput ? (
                        <>
                            <div className="relative animate-in fade-in duration-300">
                                <UserIcon className="absolute left-3 top-3 h-5 w-5 text-slate-500" />
                                <input 
                                    type="text" 
                                    value={loginInput} 
                                    placeholder="E-mail ou CPF" 
                                    onChange={e => setLoginInput(e.target.value)} 
                                    className="w-full bg-slate-900 text-white pl-10 p-3 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none transition-all" 
                                    required 
                                />
                            </div>
                            <div className="relative">
                                <Lock className="absolute left-3 top-3 h-5 w-5 text-slate-500" />
                                <input 
                                    type={showPassword ? "text" : "password"} 
                                    placeholder="Sua Senha" 
                                    value={password} 
                                    onChange={e => setPassword(e.target.value)} 
                                    className="w-full bg-slate-900 text-white pl-10 p-3 pr-10 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none transition-all" 
                                    required 
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-3 text-slate-500 hover:text-slate-300 focus:outline-none"
                                >
                                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>
                            
                            <div className="flex items-center gap-2">
                                <input 
                                    type="checkbox" 
                                    id="keepLogged" 
                                    checked={keepLogged} 
                                    onChange={e => setKeepLogged(e.target.checked)} 
                                    className="w-4 h-4 rounded border-slate-500 bg-slate-800 text-emerald-500 focus:ring-emerald-500 cursor-pointer"
                                />
                                <label htmlFor="keepLogged" className="text-sm text-slate-300 cursor-pointer select-none">
                                    Mantenha-me conectado
                                </label>
                            </div>
                        </>
                    ) : (
                        <div className="animate-in slide-in-from-right duration-300">
                            <div className="bg-emerald-900/20 p-4 rounded-lg border border-emerald-500/30 mb-4 text-center">
                                <ShieldCheck className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
                                <p className="text-emerald-400 font-bold text-sm">Autenticação em Dois Fatores</p>
                                <p className="text-slate-400 text-xs">Digite o código de 6 dígitos do seu app.</p>
                            </div>
                            <div className="relative mb-4">
                                <Key className="absolute left-3 top-3 h-5 w-5 text-emerald-500" />
                                <input 
                                    type="text" 
                                    placeholder="000 000" 
                                    value={code2FA} 
                                    onChange={e => setCode2FA(e.target.value.replace(/\D/g, '').slice(0, 6))} 
                                    className="w-full bg-slate-900 text-white pl-10 p-3 border border-emerald-500 text-center tracking-widest text-xl rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none" 
                                    autoFocus
                                    required 
                                />
                            </div>

                            <div className="flex items-center justify-center gap-2 mb-4">
                                <input 
                                    type="checkbox" 
                                    id="trustDevice" 
                                    checked={trustDevice} 
                                    onChange={e => setTrustDevice(e.target.checked)} 
                                    className="w-4 h-4 rounded border-slate-500 bg-slate-800 text-emerald-500 focus:ring-emerald-500 cursor-pointer"
                                />
                                <label htmlFor="trustDevice" className="text-xs text-slate-300 cursor-pointer select-none">
                                    Não pedir código por 30 dias neste navegador
                                </label>
                            </div>
                            <button type="button" onClick={() => { setShow2FAInput(false); setCode2FA(''); }} className="text-xs text-slate-500 hover:text-white mt-2 underline w-full text-center">Cancelar e voltar</button>
                        </div>
                    )}
                </div>
            )}

            {isLogin && !show2FAInput && (
                <div className="flex justify-end -mt-3">
                    <Link to="/forgot-password" className="text-xs text-emerald-500 hover:text-emerald-400 hover:underline transition-colors">
                        Esqueci minha senha
                    </Link>
                </div>
            )}

            {!isLogin && (
              <>
                <div className="relative animate-in slide-in-from-top-1">
                    <Lock className="absolute left-3 top-3 h-5 w-5 text-slate-500" />
                    <input 
                    type={showPassword ? "text" : "password"} 
                    placeholder="Sua Senha" 
                    value={password} 
                    onChange={e => setPassword(e.target.value)} 
                    className="w-full bg-slate-900 text-white pl-10 p-3 pr-10 border border-slate-700 rounded-lg focus:border-emerald-500 outline-none transition-all"
                    required 
                    />
                    <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 top-3 text-slate-500 hover:text-slate-300 focus:outline-none"
                    >
                        {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                </div>

                <div className="relative animate-in slide-in-from-top-1">
                    <CheckCircle className="absolute left-3 top-3 h-5 w-5 text-slate-500" />
                    <input 
                    type={showConfirmPassword ? "text" : "password"} 
                    placeholder="Confirmar Senha" 
                    value={confirmPassword} 
                    onChange={e => setConfirmPassword(e.target.value)} 
                    className={`w-full bg-slate-900 text-white pl-10 p-3 pr-10 border rounded-lg outline-none transition-all ${password && confirmPassword && password !== confirmPassword ? 'border-red-500 focus:border-red-500' : 'border-slate-700 focus:border-emerald-500'}`}
                    required 
                    />
                    <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="absolute right-3 top-3 text-slate-500 hover:text-slate-300 focus:outline-none"
                    >
                        {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                    {password && confirmPassword && password !== confirmPassword && (
                    <span className="text-xs text-red-400 absolute right-3 -bottom-5 font-bold">Não coincidem</span>
                    )}
                </div>

                <div className="bg-emerald-900/20 p-4 rounded-lg border border-emerald-500/30 mt-6">
                    <label className="text-xs font-bold text-emerald-400 uppercase flex items-center gap-1 mb-2">
                      <ShieldCheck size={14}/> Palavra-Chave do Condomínio
                    </label>
                    <input type="password" value={secretKeyword} onChange={e => setSecretKeyword(e.target.value)} className="w-full p-2 bg-slate-900 border border-emerald-500/50 rounded text-white focus:ring-2 focus:ring-emerald-500/50 outline-none" placeholder="Solicite ao síndico" required />
                </div>
              </>
            )}

            {/* --- UPDATED: TERMS AND PRIVACY LINKS --- */}
            {!isLogin && (
                <div className="flex items-start gap-2 mt-4">
                    <input 
                        type="checkbox" 
                        id="terms" 
                        checked={acceptedTerms}
                        onChange={e => setAcceptedTerms(e.target.checked)}
                        className="w-4 h-4 mt-1 rounded border-slate-500 bg-slate-800 text-emerald-500 focus:ring-emerald-500 cursor-pointer"
                    />
                    <label htmlFor="terms" className="text-xs text-slate-400 cursor-pointer">
                        Li e aceito os 
                        <button type="button" onClick={() => setShowTermsModal(true)} className="text-emerald-400 hover:underline mx-1 font-bold">Termos de Uso</button> 
                        e a 
                        <button type="button" onClick={() => setShowPrivacyModal(true)} className="text-emerald-400 hover:underline mx-1 font-bold">Política de Privacidade</button>.
                    </label>
                </div>
            )}

            <button 
              type="submit" 
              disabled={loading || (!isLogin && !acceptedTerms)} 
              className={`w-full font-bold py-3 rounded-lg transition-all shadow-lg flex items-center justify-center gap-2 mt-6 ${
                  loading || (!isLogin && !acceptedTerms)
                  ? 'bg-slate-700 text-slate-400 cursor-not-allowed'
                  : 'bg-emerald-600 hover:bg-emerald-500 text-white hover:shadow-emerald-500/20'
              }`}
            >
              {loading ? 'Processando...' : (isLogin ? (show2FAInput ? 'Verificar Código' : <>Entrar <ArrowRight size={18}/></>) : 'Finalizar Cadastro')}
            </button>

            <button type="button" onClick={handleToggleMode} className="w-full text-sm text-slate-400 hover:text-white hover:underline transition-colors mt-4">
              {isLogin 
                ? (isAffiliateContext ? 'Ainda não é parceiro? Cadastre-se' : 'Novo morador? Cadastre-se aqui')
                : 'Já possui conta? Fazer Login'
              }
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Auth;
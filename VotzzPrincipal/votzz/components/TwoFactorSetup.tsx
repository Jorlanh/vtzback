import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react'; 
import { ShieldCheck, CheckCircle, X, Copy, Eye, EyeOff } from 'lucide-react';
import api from '../services/api';

interface TwoFactorSetupProps {
    user: any;
}

export const TwoFactorSetup: React.FC<TwoFactorSetupProps> = ({ user }) => {
    const [step, setStep] = useState<'IDLE' | 'SETUP' | 'CONFIRMED'>('IDLE');
    const [qrCodeUrl, setQrCodeUrl] = useState('');
    const [secretKey, setSecretKey] = useState('');
    const [confirmationCode, setConfirmationCode] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [showSecret, setShowSecret] = useState(false);

    // Helper para extrair o segredo da URL otpauth:// se o backend não enviar separado
    const extractSecretFromUrl = (url: string) => {
        try {
            const urlObj = new URL(url);
            return urlObj.searchParams.get("secret") || "";
        } catch (e) {
            return "";
        }
    };

    const handleStartSetup = async () => {
        setLoading(true);
        setError('');
        try {
            // O backend precisa estar configurado para aceitar essa requisição com Token
            const response = await api.post('/auth/2fa/setup');
            const url = response.data.qrCodeUrl;
            
            // Tenta pegar o segredo da resposta ou extrai da URL
            const secret = response.data.secret || extractSecretFromUrl(url);
            
            setQrCodeUrl(url);
            setSecretKey(secret);
            setStep('SETUP');
        } catch (e: any) {
            console.error(e);
            // Se der 403, significa que o backend bloqueou (token inválido ou config errada)
            if (e.response?.status === 403) {
                alert('Erro de permissão (403). Verifique se você está logado ou a configuração do SecurityConfig no Java.');
            } else {
                alert('Erro ao iniciar configuração 2FA.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = async () => {
        setLoading(true);
        setError('');
        try {
            // Envia apenas os números
            await api.post('/auth/2fa/confirm', { code: parseInt(confirmationCode.replace(/\D/g, '')) });
            setStep('CONFIRMED');
        } catch (e: any) {
            setError('Código inválido. Verifique o Google Authenticator e tente novamente.');
        } finally {
            setLoading(false);
        }
    };

    const handleDisable = async () => {
        if(!window.confirm("Tem certeza que deseja desativar a proteção extra?")) return;
        try {
            await api.post('/auth/2fa/disable');
            setStep('IDLE');
            setQrCodeUrl('');
            setSecretKey('');
            setConfirmationCode('');
            alert("2FA Desativado.");
        } catch(e) { alert("Erro ao desativar."); }
    };

    const copyToClipboard = () => {
        navigator.clipboard.writeText(secretKey);
        alert("Código copiado!");
    };

    return (
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm mt-6">
            <div className="flex items-start gap-4">
                <div className="bg-emerald-100 p-3 rounded-full">
                    <ShieldCheck className="w-6 h-6 text-emerald-600" />
                </div>
                <div className="flex-1">
                    <h3 className="font-bold text-slate-800 text-lg">Autenticação em Duas Etapas</h3>
                    <p className="text-slate-500 text-sm mb-4">
                        Proteja sua conta exigindo um código do Google Authenticator ao logar.
                    </p>

                    {step === 'IDLE' && (
                        <div className="flex gap-2">
                             <button 
                                onClick={handleStartSetup} 
                                disabled={loading}
                                className="bg-slate-900 text-white px-4 py-2 rounded-lg text-sm font-bold hover:bg-slate-800 transition-colors"
                            >
                                {loading ? 'Carregando...' : 'Configurar / Ativar 2FA'}
                            </button>
                            <button 
                                onClick={handleDisable}
                                className="text-red-500 text-sm font-bold px-4 py-2 hover:bg-red-50 rounded-lg transition-colors"
                            >
                                Desativar
                            </button>
                        </div>
                    )}

                    {step === 'SETUP' && (
                        <div className="bg-slate-50 p-6 rounded-xl border border-slate-200 mt-2 animate-in fade-in">
                            <h4 className="font-bold text-slate-800 mb-4 border-b pb-2">Configuração</h4>
                            
                            <div className="grid md:grid-cols-2 gap-8 items-center">
                                {/* QR CODE */}
                                <div className="text-center space-y-3">
                                    <div className="bg-white p-4 rounded-xl shadow-sm inline-block">
                                        <QRCodeSVG value={qrCodeUrl} size={160} />
                                    </div>
                                    <p className="text-xs text-slate-500 font-medium">Abra o Google Authenticator e escaneie.</p>
                                </div>

                                {/* MANUAL CODE */}
                                <div className="space-y-6">
                                    <div className="space-y-2">
                                        <p className="text-sm font-bold text-slate-700 flex justify-between items-center">
                                            Não consegue ler o QR?
                                            <button onClick={() => setShowSecret(!showSecret)} className="text-emerald-600 text-xs hover:underline flex items-center gap-1">
                                                {showSecret ? <><EyeOff size={12}/> Ocultar</> : <><Eye size={12}/> Mostrar código</>}
                                            </button>
                                        </p>
                                        
                                        {showSecret ? (
                                            <div className="flex items-center gap-2 bg-white border border-slate-300 rounded-lg p-2">
                                                <code className="flex-1 font-mono text-center text-slate-800 tracking-wider font-bold select-all text-sm break-all">
                                                    {secretKey}
                                                </code>
                                                <button onClick={copyToClipboard} className="p-2 hover:bg-slate-100 rounded text-slate-500" title="Copiar">
                                                    <Copy size={16} />
                                                </button>
                                            </div>
                                        ) : (
                                            <div className="bg-slate-200 h-10 rounded-lg animate-pulse text-center text-xs text-slate-400 flex items-center justify-center">
                                                Código oculto
                                            </div>
                                        )}
                                    </div>

                                    <div className="space-y-2">
                                        <label className="text-sm font-bold text-slate-700 block">Digite o código gerado (6 dígitos):</label>
                                        <div className="flex gap-2">
                                            <input 
                                                type="text" 
                                                maxLength={6}
                                                placeholder="000 000"
                                                className="flex-1 p-2.5 border border-slate-300 rounded-lg text-center tracking-widest font-mono text-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                                                value={confirmationCode}
                                                onChange={e => setConfirmationCode(e.target.value.replace(/\D/g, ''))}
                                            />
                                            <button 
                                                onClick={handleConfirm}
                                                disabled={confirmationCode.length < 6 || loading}
                                                className="bg-emerald-600 text-white px-6 rounded-lg font-bold hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                                            >
                                                {loading ? '...' : 'Validar'}
                                            </button>
                                        </div>
                                        {error && <p className="text-red-500 text-xs font-bold flex items-center gap-1"><X size={12}/> {error}</p>}
                                    </div>
                                </div>
                            </div>
                            
                            <div className="mt-6 text-center">
                                <button onClick={() => setStep('IDLE')} className="text-xs text-slate-500 hover:text-red-500 transition-colors underline">
                                    Cancelar configuração
                                </button>
                            </div>
                        </div>
                    )}

                    {step === 'CONFIRMED' && (
                        <div className="flex items-center gap-3 text-emerald-700 font-bold bg-emerald-50 p-4 rounded-xl border border-emerald-200 mt-2 animate-in zoom-in-95">
                            <div className="bg-white p-2 rounded-full shadow-sm">
                                <CheckCircle size={24} className="text-emerald-500" />
                            </div>
                            <div>
                                <p>Segurança Ativada com Sucesso!</p>
                                <p className="text-xs font-normal opacity-80">Sua conta agora está protegida em todos os acessos.</p>
                            </div>
                            <button onClick={() => setStep('IDLE')} className="ml-auto text-emerald-800 text-sm hover:underline font-bold px-3 py-1 bg-emerald-100 rounded-lg">OK</button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
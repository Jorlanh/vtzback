import React from 'react';
import { AlertTriangle, Calendar, CheckCircle } from 'lucide-react';
import { differenceInDays, format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

interface Props {
  expirationDate: string; // Formato YYYY-MM-DD
  userRole: string;
  onRenewClick: () => void;
}

export const SubscriptionStatus: React.FC<Props> = ({ expirationDate, userRole, onRenewClick }) => {
  const today = new Date();
  const expiry = new Date(expirationDate);
  const daysLeft = differenceInDays(expiry, today);

  // Se o plano estiver VENCIDO
  if (daysLeft < 0) {
    return (
      <div className="bg-red-500/10 border border-red-500 rounded-lg p-4 mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <AlertTriangle className="text-red-500 h-8 w-8" />
          <div>
            <h3 className="font-bold text-red-500">Assinatura Suspensa</h3>
            <p className="text-red-400 text-sm">
              O plano venceu em {format(expiry, "dd 'de' MMMM 'de' yyyy", { locale: ptBR })}. 
              As funcionalidades estão bloqueadas.
            </p>
          </div>
        </div>
        {userRole === 'SINDICO' && (
          <button 
            onClick={onRenewClick}
            className="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-4 rounded transition-colors"
          >
            Renovar Agora e Desbloquear
          </button>
        )}
      </div>
    );
  }

  // Se o plano estiver PERTO DE VENCER (ex: faltam 15 dias ou menos)
  if (daysLeft <= 30) {
    return (
      <div className="bg-amber-500/10 border border-amber-500/50 rounded-lg p-4 mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Calendar className="text-amber-500 h-6 w-6" />
          <div>
            <h3 className="font-bold text-amber-500">Renovação Necessária em Breve</h3>
            <p className="text-amber-400 text-sm">
              Seu plano vence em {daysLeft} dias ({format(expiry, "dd/MM/yyyy")}).
              Evite bloqueios.
            </p>
          </div>
        </div>
        {userRole === 'SINDICO' && (
          <button 
            onClick={onRenewClick}
            className="bg-amber-500 hover:bg-amber-600 text-white font-bold py-2 px-4 rounded transition-colors text-sm"
          >
            Renovar Antecipado (+12 meses)
          </button>
        )}
      </div>
    );
  }

  // Plano OK (só mostra nos settings, mas aqui retornamos null para não poluir a home)
  return null;
};
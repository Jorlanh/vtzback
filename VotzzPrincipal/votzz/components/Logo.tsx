
import React from 'react';
import { Vote } from 'lucide-react';

interface LogoProps {
  theme?: 'light' | 'dark';
  className?: string;
  size?: 'sm' | 'md' | 'lg';
  showSlogan?: boolean;
}

export const Logo: React.FC<LogoProps> = ({ theme = 'dark', className = '', size = 'md', showSlogan = false }) => {
  const isLightText = theme === 'light';
  
  const dimensions = {
    sm: { box: 'w-8 h-8', icon: 'w-4 h-4', text: 'text-xl', offset: 'border-b-2', slogan: 'text-[0.5rem]' },
    md: { box: 'w-11 h-11', icon: 'w-6 h-6', text: 'text-2xl', offset: 'border-b-4', slogan: 'text-[0.6rem]' },
    lg: { box: 'w-16 h-16', icon: 'w-9 h-9', text: 'text-5xl', offset: 'border-b-[6px]', slogan: 'text-xs' },
  };

  const dim = dimensions[size];

  return (
    <div className={`flex items-center gap-3.5 group select-none ${className}`}>
      {/* Icon Container - Modern 3D "Urna" Box Style */}
      <div className={`relative flex items-center justify-center ${dim.box} transition-transform duration-200 group-hover:translate-y-[2px]`}>
        
        {/* Glow effect (Behind) */}
        <div className="absolute -inset-2 bg-emerald-500/20 rounded-2xl blur-lg opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
        
        {/* The Box (Urna) */}
        <div className={`
          absolute inset-0 
          bg-gradient-to-br from-emerald-400 via-emerald-500 to-emerald-700 
          rounded-xl 
          ${dim.offset} border-emerald-800 
          shadow-[0_2px_10px_rgba(16,185,129,0.2)]
          flex items-center justify-center
          overflow-hidden
        `}>
          
          {/* Top Glass Highlight */}
          <div className="absolute top-0 left-0 right-0 h-[40%] bg-gradient-to-b from-white/20 to-transparent" />
          
          {/* The Icon (Vote/Ballot) */}
          <Vote 
            className={`
              relative z-10 ${dim.icon} text-white drop-shadow-md 
              transform transition-all duration-300 
              group-hover:scale-110 group-hover:-rotate-6
            `} 
            strokeWidth={2.5} 
          />
        </div>
      </div>
      
      {/* Text Brand */}
      <div className="flex flex-col justify-center">
        <span className={`
          ${dim.text} font-black tracking-tighter 
          ${isLightText ? 'text-white' : 'text-slate-800'} 
          flex items-baseline transition-colors duration-300 leading-none
        `}>
          Votzz
          <span className="text-emerald-500 ml-0.5 animate-pulse text-[1.2em] leading-none">.</span>
        </span>
        {showSlogan && (
          <span className={`${dim.slogan} font-medium tracking-widest uppercase ${isLightText ? 'text-emerald-300' : 'text-emerald-700'} mt-0.5`}>
            Decisões Inteligentes
          </span>
        )}
      </div>
    </div>
  );
};

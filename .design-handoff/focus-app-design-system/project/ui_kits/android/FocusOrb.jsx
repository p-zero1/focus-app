// FocusOrb.jsx — the centerpiece. Plasma orb with breathing animation.
function FocusOrb({ size = 240, state = 'idle', mode = 'focus' }) {
  const colors = {
    focus:    { core:'#C8C2FF', mid:'#8E85FF', deep:'#3A2F8F', glow:'rgba(142,133,255,0.55)' },
    chill:    { core:'#FFE5B0', mid:'#FFB347', deep:'#8F5A1F', glow:'rgba(255,179,71,0.5)' },
    hardcore: { core:'#FFFFFF', mid:'#BBBBBB', deep:'#444444', glow:'rgba(255,255,255,0.3)' },
  };
  const stateColors = {
    distract: { core:'#FFC8C8', mid:'#FF6B6B', deep:'#8F2A2A', glow:'rgba(255,107,107,0.5)' },
    success:  { core:'#C5F0C8', mid:'#4CAF50', deep:'#1F5F22', glow:'rgba(76,175,80,0.5)' },
    break:    { core:'#A8E5D0', mid:'#4CAF92', deep:'#1F5F4A', glow:'rgba(76,175,146,0.5)' },
  };
  const c = stateColors[state] || colors[mode];
  const breathing = state !== 'collapse';
  return (
    <div style={{ width:size, height:size, position:'relative', display:'flex', alignItems:'center', justifyContent:'center' }}>
      <div style={{ position:'absolute', inset:0, borderRadius:'50%', border:'1px solid rgba(108,99,255,0.12)' }} />
      <div style={{ position:'absolute', inset:size*0.12, borderRadius:'50%', border:'1px solid rgba(108,99,255,0.18)' }} />
      <div style={{
        width:size*0.62, height:size*0.62, borderRadius:'50%',
        background:`radial-gradient(circle at 38% 32%, ${c.core} 0%, ${c.mid} 55%, ${c.deep} 100%)`,
        boxShadow:`0 0 ${size*0.5}px ${size*0.05}px ${c.glow}, inset 0 0 ${size*0.18}px rgba(255,255,255,0.22)`,
        animation: breathing ? 'orbBreath 4s ease-in-out infinite' : 'none',
        transform: state==='collapse' ? 'scale(0.4)' : undefined,
        opacity: state==='collapse' ? 0.3 : 1,
        transition:'all 800ms cubic-bezier(0.65,0,0.35,1)',
      }}/>
    </div>
  );
}
Object.assign(window, { FocusOrb });

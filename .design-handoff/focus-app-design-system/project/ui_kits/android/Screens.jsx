// Screens.jsx — All Focus App screens. Imports FocusOrb.
const I = ({ name, size=24, fill=0, color='currentColor' }) => (
  <span className="material-symbols-rounded" style={{
    fontSize:size, color, fontVariationSettings:`'FILL' ${fill}, 'wght' 400, 'opsz' 24`,
    userSelect:'none', lineHeight:1,
  }}>{name}</span>
);

// ── Bottom nav ──────────────────────────────────────────────
function BottomNav({ tab, setTab }) {
  const tabs = [
    { id:'timer', label:'Timer', icon:'home' },
    { id:'analytics', label:'Analytics', icon:'bar_chart' },
    { id:'history', label:'History', icon:'list' },
    { id:'profile', label:'Profile', icon:'person' },
  ];
  return (
    <div style={{
      display:'flex', height:64, background:'rgba(26,26,46,0.92)',
      backdropFilter:'blur(24px)', WebkitBackdropFilter:'blur(24px)',
      borderTop:'1px solid rgba(255,255,255,0.06)',
      flexShrink:0,
    }}>
      {tabs.map(t => {
        const on = tab === t.id;
        return (
          <button key={t.id} onClick={()=>setTab(t.id)} style={{
            flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', gap:3,
            background:'transparent', border:'none', cursor:'pointer',
            color: on ? '#6C63FF' : '#9E9E9E',
            fontFamily:'Inter,sans-serif', fontSize:11, fontWeight:500,
            textShadow: on ? '0 0 14px rgba(108,99,255,0.6)' : 'none',
          }}>
            <I name={t.icon} size={26} fill={on?1:0}/>
            <span>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ── Timer screen ────────────────────────────────────────────
function TimerScreen({ session, setSession, mode }) {
  const [selectedMode, setSelectedMode] = React.useState('Pomodoro');
  const [strict, setStrict] = React.useState('Strict');
  const modes = ['Pomodoro','Deep Work','Study','Custom'];
  const stricts = ['Relaxed','Strict','Hardcore'];

  if (session.active) {
    return (
      <div style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', padding:'30px 20px 20px', position:'relative', overflow:'hidden' }}>
        {session.distracted && (
          <div style={{
            position:'absolute', top:16, left:16, right:16, padding:'14px 16px',
            background:'rgba(255,107,107,0.18)', backdropFilter:'blur(16px)', WebkitBackdropFilter:'blur(16px)',
            border:'1px solid rgba(255,107,107,0.4)', borderRadius:16,
            boxShadow:'0 0 32px rgba(255,107,107,0.2)', display:'flex', gap:12, alignItems:'center', zIndex:10,
          }}>
            <I name="notifications_active" size={22} fill={1} color="#FF8E8E"/>
            <div style={{ flex:1, fontSize:13, fontWeight:600, color:'#fff' }}>You broke focus after 6 min 😅</div>
          </div>
        )}
        <div style={{ marginTop:60 }}>
          <FocusOrb size={260} state={session.distracted?'distract':null} mode={mode}/>
        </div>
        <div style={{ position:'absolute', top:'50%', left:0, right:0, transform:'translateY(-50%)', textAlign:'center', pointerEvents:'none' }}>
          <div style={{ fontSize:64, fontWeight:700, color:'#fff', letterSpacing:'-0.02em', fontVariantNumeric:'tabular-nums', lineHeight:1, textShadow:'0 0 20px rgba(0,0,0,0.5)' }}>{session.time}</div>
          <div style={{ fontSize:13, color:'rgba(255,255,255,0.6)', marginTop:6 }}>{session.elapsed} elapsed</div>
        </div>
        {session.distractions > 0 && (
          <div style={{ display:'flex', gap:6, alignItems:'center', marginTop:'auto', marginBottom:16, padding:'6px 12px', background:'rgba(255,107,107,0.15)', borderRadius:999 }}>
            <span style={{ width:8, height:8, borderRadius:'50%', background:'#FF6B6B' }}/>
            <span style={{ fontSize:12, color:'#FF8E8E', fontWeight:600 }}>{session.distractions}</span>
          </div>
        )}
        <div style={{ display:'flex', gap:14, marginBottom:8 }}>
          <button onClick={()=>setSession({...session, active:false})} style={{
            width:52, height:52, borderRadius:'50%',
            background:'rgba(255,255,255,0.06)', backdropFilter:'blur(20px)', WebkitBackdropFilter:'blur(20px)',
            border:'1px solid rgba(255,255,255,0.14)', color:'#fff', display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer',
          }}><I name="pause" size={22} fill={1}/></button>
          <button onClick={()=>setSession({ active:false, finished:true, ...session })} style={{
            width:52, height:52, borderRadius:'50%',
            background:'radial-gradient(circle at 35% 30%, #FFB5B5, #FF6B6B 60%, #8F2A2A)',
            boxShadow:'0 0 0 1px rgba(255,255,255,0.18) inset, 0 0 28px rgba(255,107,107,0.45)',
            border:'none', color:'#fff', display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer',
          }}><I name="stop" size={22} fill={1}/></button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ flex:1, display:'flex', flexDirection:'column', padding:'24px 20px 20px', gap:20, overflow:'auto' }}>
      <div style={{ display:'flex', justifyContent:'center', marginTop:8 }}>
        <FocusOrb size={200} mode={mode}/>
      </div>
      <div style={{ textAlign:'center', marginTop:-160, position:'relative', zIndex:1, pointerEvents:'none' }}>
        <div style={{ fontSize:56, fontWeight:700, color:'#fff', letterSpacing:'-0.02em', fontVariantNumeric:'tabular-nums', lineHeight:1 }}>00:00</div>
        <div style={{ fontSize:12, color:'rgba(255,255,255,0.5)', marginTop:4, letterSpacing:'0.08em', textTransform:'uppercase', fontWeight:600 }}>Ready</div>
      </div>
      <div style={{ marginTop:30, display:'flex', gap:6, overflowX:'auto', paddingBottom:4 }}>
        {modes.map(m=> (
          <button key={m} onClick={()=>setSelectedMode(m)} style={{
            height:34, padding:'0 14px', borderRadius:999, fontSize:13, fontWeight:500, fontFamily:'Inter,sans-serif',
            background: selectedMode===m ? '#6C63FF' : 'transparent',
            color: selectedMode===m ? '#fff' : '#9E9E9E',
            border:`1px solid ${selectedMode===m?'#6C63FF':'rgba(255,255,255,0.06)'}`,
            boxShadow: selectedMode===m?'0 0 16px rgba(108,99,255,0.35)':'none',
            cursor:'pointer', whiteSpace:'nowrap', flexShrink:0,
          }}>{m}</button>
        ))}
      </div>
      <div style={{ display:'flex', gap:14, justifyContent:'center', alignItems:'center', padding:'16px', background:'#1A1A2E', borderRadius:16, border:'1px solid rgba(255,255,255,0.06)' }}>
        <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:2, minWidth:54 }}>
          <div style={{ fontSize:13, color:'#5C5C70', fontVariantNumeric:'tabular-nums' }}>0</div>
          <div style={{ fontSize:24, fontWeight:700, color:'#fff', fontVariantNumeric:'tabular-nums' }}>0</div>
          <div style={{ fontSize:13, color:'#5C5C70', fontVariantNumeric:'tabular-nums' }}>1</div>
          <div style={{ fontSize:10, letterSpacing:'0.08em', textTransform:'uppercase', color:'#9E9E9E', fontWeight:600, marginTop:4 }}>Hours</div>
        </div>
        <div style={{ fontSize:24, color:'#5C5C70', fontWeight:700 }}>:</div>
        <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:2, minWidth:54 }}>
          <div style={{ fontSize:13, color:'#5C5C70', fontVariantNumeric:'tabular-nums' }}>20</div>
          <div style={{ fontSize:24, fontWeight:700, color:'#fff', fontVariantNumeric:'tabular-nums' }}>25</div>
          <div style={{ fontSize:13, color:'#5C5C70', fontVariantNumeric:'tabular-nums' }}>30</div>
          <div style={{ fontSize:10, letterSpacing:'0.08em', textTransform:'uppercase', color:'#9E9E9E', fontWeight:600, marginTop:4 }}>Minutes</div>
        </div>
      </div>
      <div style={{ display:'flex', gap:6, justifyContent:'center' }}>
        {stricts.map(s=>(
          <button key={s} onClick={()=>setStrict(s)} style={{
            height:30, padding:'0 14px', borderRadius:999, fontSize:12, fontWeight:500, fontFamily:'Inter,sans-serif',
            background: strict===s?'#6C63FF':'transparent', color: strict===s?'#fff':'#9E9E9E',
            border:`1px solid ${strict===s?'#6C63FF':'rgba(255,255,255,0.06)'}`,
            cursor:'pointer',
          }}>{s}</button>
        ))}
      </div>
      <button onClick={()=>setSession({ active:true, time:'24:38', elapsed:'00:22', distractions:0, distracted:false })} style={{
        width:'100%', height:60, borderRadius:18, color:'#fff', fontSize:16, fontWeight:700, fontFamily:'Inter,sans-serif', letterSpacing:'-0.01em',
        background:'radial-gradient(120% 200% at 0% 0%, rgba(255,255,255,0.22), transparent 40%), linear-gradient(180deg, #837BFF 0%, #6C63FF 50%, #5A52E0 100%)',
        boxShadow:'0 0 0 1px rgba(255,255,255,0.14) inset, 0 1px 0 0 rgba(255,255,255,0.35) inset, 0 8px 32px rgba(108,99,255,0.45), 0 2px 8px rgba(0,0,0,0.4)',
        border:'none', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', gap:10, marginTop:'auto',
      }}>
        <I name="play_arrow" size={22} fill={1}/>Start session
      </button>
    </div>
  );
}

// ── Analytics screen ────────────────────────────────────────
function AnalyticsScreen() {
  const [seg, setSeg] = React.useState('Daily');
  const days = [
    { d:'04-30', h: 30 }, { d:'05-01', h: 70 }, { d:'05-02', h: 45 },
    { d:'05-03', h: 90 }, { d:'05-04', h: 60 }, { d:'05-05', h: 110 }, { d:'05-06', h: 80 },
  ];
  const max = 120;
  return (
    <div style={{ flex:1, padding:'20px', overflow:'auto', display:'flex', flexDirection:'column', gap:18 }}>
      <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between' }}>
        <h1 style={{ fontSize:28, fontWeight:700, color:'#fff', margin:0, letterSpacing:'-0.02em' }}>Analytics</h1>
        <div style={{ display:'flex', background:'#1A1A2E', borderRadius:999, padding:3, border:'1px solid rgba(255,255,255,0.06)' }}>
          {['Daily','Weekly'].map(s=>(
            <button key={s} onClick={()=>setSeg(s)} style={{
              padding:'6px 14px', borderRadius:999, fontSize:12, fontWeight:500, fontFamily:'Inter,sans-serif',
              background: seg===s?'#6C63FF':'transparent', color: seg===s?'#fff':'#9E9E9E', border:'none', cursor:'pointer',
              boxShadow: seg===s?'0 0 12px rgba(108,99,255,0.35)':'none',
            }}>{s}</button>
          ))}
        </div>
      </div>
      <div style={{ background:'#1A1A2E', borderRadius:16, padding:18, border:'1px solid rgba(255,255,255,0.06)', boxShadow:'0 4px 16px rgba(0,0,0,0.32)' }}>
        <div style={{ fontSize:11, letterSpacing:'0.08em', textTransform:'uppercase', color:'#9E9E9E', fontWeight:600, marginBottom:14 }}>Focus minutes</div>
        <div style={{ display:'flex', alignItems:'flex-end', gap:8, height:140 }}>
          {days.map((d,i)=>(
            <div key={i} style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', gap:6, justifyContent:'flex-end' }}>
              <div style={{ width:'100%', height:`${(d.h/max)*100}%`, background:'linear-gradient(180deg, #837BFF, #6C63FF)', borderRadius:'6px 6px 2px 2px', boxShadow:'0 0 12px rgba(108,99,255,0.35)' }}/>
              <div style={{ fontSize:10, color:'#5C5C70', fontVariantNumeric:'tabular-nums' }}>{d.d.slice(3)}</div>
            </div>
          ))}
        </div>
      </div>
      <div style={{ display:'flex', gap:12 }}>
        <div style={{ flex:1, background:'#1A1A2E', borderRadius:16, padding:18, border:'1px solid rgba(255,255,255,0.06)', boxShadow:'0 4px 16px rgba(0,0,0,0.32)', display:'flex', flexDirection:'column', alignItems:'center', gap:10 }}>
          <div style={{ fontSize:11, letterSpacing:'0.08em', textTransform:'uppercase', color:'#9E9E9E', fontWeight:600 }}>Focus score</div>
          <div style={{ width:88, height:88, borderRadius:'50%', background:'conic-gradient(#4CAF50 0 76%, rgba(255,255,255,0.08) 76%)', display:'flex', alignItems:'center', justifyContent:'center', boxShadow:'0 0 24px rgba(76,175,80,0.3)' }}>
            <div style={{ width:70, height:70, borderRadius:'50%', background:'#1A1A2E', display:'flex', alignItems:'center', justifyContent:'center', flexDirection:'column' }}>
              <div style={{ fontSize:24, fontWeight:700, color:'#fff' }}>76</div>
            </div>
          </div>
          <div style={{ fontSize:12, color:'#9E9E9E' }}>This week</div>
        </div>
        <div style={{ flex:1, background:'#1A1A2E', borderRadius:16, padding:18, border:'1px solid rgba(255,255,255,0.06)', boxShadow:'0 4px 16px rgba(0,0,0,0.32)', display:'flex', flexDirection:'column', gap:8 }}>
          <div style={{ display:'flex', alignItems:'center', gap:8 }}>
            <I name="schedule" size={18} color="#FFB347"/>
            <div style={{ fontSize:11, letterSpacing:'0.08em', textTransform:'uppercase', color:'#9E9E9E', fontWeight:600 }}>Best time</div>
          </div>
          <div style={{ fontSize:18, fontWeight:600, color:'#fff', lineHeight:1.3 }}>10 AM – 12 PM</div>
          <div style={{ fontSize:12, color:'#9E9E9E', lineHeight:1.4 }}>You focus best in the late morning.</div>
        </div>
      </div>
    </div>
  );
}

// ── History screen ──────────────────────────────────────────
function HistoryScreen() {
  const [tag, setTag] = React.useState('All');
  const tags = ['All','DSA','Project X','Reading'];
  const sessions = [
    { mode:'Pomodoro', icon:'bolt', tag:'DSA', dur:'45 min', dist:2, when:'May 6 · 10:30 AM' },
    { mode:'Deep Work', icon:'psychology', tag:'Project X', dur:'1h 30m', dist:0, when:'May 6 · 8:15 AM' },
    { mode:'Study', icon:'school', tag:'DSA', dur:'25 min', dist:5, when:'May 5 · 9:50 PM' },
    { mode:'Pomodoro', icon:'bolt', tag:'Reading', dur:'25 min', dist:1, when:'May 5 · 7:30 PM' },
    { mode:'Custom', icon:'tune', tag:'DSA', dur:'1h 10m', dist:3, when:'May 5 · 2:00 PM' },
  ];
  const filtered = tag==='All' ? sessions : sessions.filter(s=>s.tag===tag);
  return (
    <div style={{ flex:1, padding:'20px 0 20px', overflow:'auto', display:'flex', flexDirection:'column', gap:14 }}>
      <h1 style={{ fontSize:28, fontWeight:700, color:'#fff', margin:'0 20px', letterSpacing:'-0.02em' }}>History</h1>
      <div style={{ display:'flex', gap:8, padding:'0 20px', overflowX:'auto' }}>
        {tags.map(t=>(
          <button key={t} onClick={()=>setTag(t)} style={{
            height:30, padding:'0 14px', borderRadius:999, fontSize:12, fontWeight:500, fontFamily:'Inter,sans-serif',
            background: tag===t ? 'rgba(108,99,255,0.16)' : 'transparent',
            color: tag===t ? '#B5AFFF' : '#9E9E9E',
            border:`1px solid ${tag===t?'rgba(108,99,255,0.3)':'rgba(255,255,255,0.06)'}`,
            cursor:'pointer', whiteSpace:'nowrap', flexShrink:0,
          }}>{t}</button>
        ))}
      </div>
      {tag !== 'All' && (
        <div style={{ margin:'0 20px', padding:'10px 14px', background:'rgba(108,99,255,0.08)', borderRadius:12, fontSize:13, color:'#B5AFFF', border:'1px solid rgba(108,99,255,0.18)' }}>
          {tag} — {filtered.length} session{filtered.length!==1?'s':''} · {filtered.length>0?'2h 50m total':'0 min total'}
        </div>
      )}
      <div style={{ display:'flex', flexDirection:'column', gap:10, padding:'0 20px' }}>
        {filtered.map((s,i)=>(
          <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:14, background:'#1A1A2E', borderRadius:14, border:'1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ width:38, height:38, borderRadius:10, background:'rgba(108,99,255,0.16)', color:'#B5AFFF', display:'flex', alignItems:'center', justifyContent:'center' }}>
              <I name={s.icon} size={20} fill={1}/>
            </div>
            <div style={{ flex:1, display:'flex', flexDirection:'column', gap:4 }}>
              <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                <span style={{ fontSize:14, fontWeight:600, color:'#fff' }}>{s.mode}</span>
                <span style={{ fontSize:11, padding:'2px 8px', background:'rgba(108,99,255,0.16)', color:'#B5AFFF', borderRadius:999 }}>{s.tag}</span>
              </div>
              <div style={{ fontSize:11, color:'#9E9E9E' }}>{s.when}</div>
            </div>
            <div style={{ display:'flex', flexDirection:'column', alignItems:'flex-end', gap:3 }}>
              <div style={{ fontSize:13, fontWeight:600, color:'#fff' }}>{s.dur}</div>
              <div style={{ fontSize:11, color:'#9E9E9E', display:'flex', alignItems:'center', gap:4 }}>
                <span style={{ color:s.dist>0?'#FF6B6B':'#4CAF50' }}>●</span>{s.dist}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Profile screen ──────────────────────────────────────────
function ProfileScreen() {
  const badges = [
    { name:'First Focus', src:'../../assets/badge-first-focus.svg', earned:true },
    { name:'Deep Diver', src:'../../assets/badge-deep-diver.svg', earned:true },
    { name:'Streak Starter', src:'../../assets/badge-streak-starter.svg', earned:true },
    { name:'Streak Master', src:'../../assets/badge-streak-master.svg', earned:false },
    { name:'Clean Hour', src:'../../assets/badge-clean-hour.svg', earned:false },
  ];
  return (
    <div style={{ flex:1, padding:'20px', overflow:'auto', display:'flex', flexDirection:'column', gap:18 }}>
      <h1 style={{ fontSize:28, fontWeight:700, color:'#fff', margin:0, letterSpacing:'-0.02em' }}>Profile</h1>
      <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:6, padding:'24px 0' }}>
        <div style={{ fontSize:56, lineHeight:1, filter:'drop-shadow(0 0 24px rgba(255,140,40,0.5))' }}>🔥</div>
        <div style={{ fontSize:48, fontWeight:800, color:'#fff', letterSpacing:'-0.02em', fontVariantNumeric:'tabular-nums', lineHeight:1 }}>7</div>
        <div style={{ fontSize:12, letterSpacing:'0.08em', textTransform:'uppercase', color:'#9E9E9E', fontWeight:600 }}>Day streak</div>
      </div>
      <div style={{ background:'#1A1A2E', borderRadius:16, padding:18, border:'1px solid rgba(255,255,255,0.06)', display:'flex', flexDirection:'column', gap:10 }}>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'baseline' }}>
          <div style={{ fontSize:14, fontWeight:600, color:'#fff' }}>Level 4</div>
          <div style={{ fontSize:12, color:'#FFB347', fontWeight:600 }}>1,240 / 1,500 XP</div>
        </div>
        <div style={{ height:8, borderRadius:999, background:'rgba(255,255,255,0.06)', overflow:'hidden' }}>
          <div style={{ width:'82%', height:'100%', background:'linear-gradient(90deg, #FFB347, #FF8C42)', boxShadow:'0 0 12px rgba(255,179,71,0.5)' }}/>
        </div>
      </div>
      <div style={{ background:'#1A1A2E', borderRadius:16, padding:18, border:'1px solid rgba(255,255,255,0.06)', display:'flex', alignItems:'center', gap:14 }}>
        <div style={{ width:64, height:64, borderRadius:'50%', background:'conic-gradient(#6C63FF 0 67%, rgba(255,255,255,0.08) 67%)', display:'flex', alignItems:'center', justifyContent:'center' }}>
          <div style={{ width:50, height:50, borderRadius:'50%', background:'#1A1A2E', display:'flex', alignItems:'center', justifyContent:'center', fontSize:13, fontWeight:700, color:'#fff' }}>2h</div>
        </div>
        <div style={{ flex:1 }}>
          <div style={{ fontSize:14, fontWeight:600, color:'#fff' }}>Daily goal</div>
          <div style={{ fontSize:12, color:'#9E9E9E', marginTop:2 }}>2h / 3h goal today</div>
        </div>
      </div>
      <div style={{ fontSize:11, letterSpacing:'0.08em', textTransform:'uppercase', color:'#9E9E9E', fontWeight:600, marginTop:4 }}>Badges</div>
      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:10 }}>
        {badges.map((b,i)=>(
          <div key={i} style={{ padding:14, background:'#1A1A2E', borderRadius:14, border:'1px solid rgba(255,255,255,0.06)', display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
            <img src={b.src} alt="" style={{ width:56, height:56, filter: b.earned ? 'none' : 'grayscale(1) brightness(0.45)' }}/>
            <div style={{ fontSize:11, fontWeight:600, color: b.earned?'#fff':'#5C5C70', textAlign:'center' }}>{b.name}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { BottomNav, TimerScreen, AnalyticsScreen, HistoryScreen, ProfileScreen, I });

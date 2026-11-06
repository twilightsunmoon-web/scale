const fs = require('fs');
const h = fs.readFileSync('index.html', 'utf8');

const seed = {
  v:1, createdAt: Date.now(),
  config:{ set:true, lang:'zh', limitMin:300, energyThr:200, attentionThr:200,
    nextWindow:{date:'', start:'20:00', end:'22:00' }, noshowPolicy:'postpone' },
  presets:[{id:'p_phone', name:'玩手机', column:'high', energy:2, attention:3, naturalEnd:false}],
  entries:[], blocks:[], days:{}
};

const boot = '<scr' + 'ipt>try{localStorage.setItem("scale.ledger.v1", ' +
  JSON.stringify(JSON.stringify(seed)) + ');}catch(e){}<\/scr' + 'ipt>';

const probeBody = `
var log = [];
function step(n, fn){ try { fn(); log.push(n + ':ok'); } catch(e){ log.push(n + ':ERR ' + e.message); } }
var q = function(s){ return document.querySelector(s); };

step('toSettings', function(){ q('[data-act="go-settings"]').click(); });
step('openCalc', function(){ q('[data-act="open-calc"]').click(); });
step('hasLink', function(){
  var b = q('[data-act="toggle-help"]');
  if (!b) throw new Error('no link');
  log.push('linkText=' + b.textContent.trim());
  log.push('boxHidden=' + document.getElementById('calcHelpBox').hidden);
  log.push('linkColor=' + getComputedStyle(b).color);
});
step('openHelp', function(){ q('[data-act="toggle-help"]').click(); });
step('afterOpen', function(){
  log.push('hidden2=' + document.getElementById('calcHelpBox').hidden);
  log.push('linkText2=' + q('[data-act="toggle-help"]').textContent.trim());
  log.push('boxHeight=' + document.getElementById('calcHelpBox').offsetHeight);
});
step('closeHelp', function(){ q('[data-act="toggle-help"]').click(); });
step('afterClose', function(){
  log.push('hidden3=' + document.getElementById('calcHelpBox').hidden);
  log.push('linkText3=' + q('[data-act="toggle-help"]').textContent.trim());
});
document.title = log.join(' | ');
`;

const probe = '<scr' + 'ipt>' + probeBody + '<\/scr' + 'ipt>';
fs.writeFileSync('_h.html', h.replace('<script>', boot + '\n<script>').replace('</body>', probe + '</body>'));

// 展开状态截图用
const shotProbe = '<scr' + 'ipt>' +
  'document.querySelector(\'[data-act="go-settings"]\').click();' +
  'document.querySelector(\'[data-act="open-calc"]\').click();' +
  'document.querySelector(\'[data-act="toggle-help"]\').click();' +
  '<\/scr' + 'ipt>';
fs.writeFileSync('_h2.html', h.replace('<script>', boot + '\n<script>').replace('</body>', shotProbe + '</body>'));
fs.writeFileSync('_h3.html', '<!DOCTYPE html><html><body style="margin:0;background:#fff">' +
  '<iframe src="_h2.html" style="width:390px;height:844px;border:0;display:block"></iframe></body></html>');
console.log('built');

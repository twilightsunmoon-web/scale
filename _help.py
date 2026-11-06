# -*- coding: utf-8 -*-
import io

p = 'index.html'
s = io.open(p, encoding='utf-8').read()

# ---------- 1. 颜色变量：加一个蓝 ----------
s = s.replace("  --calm:#33566a; --calm-ink:#ffffff; --amber:#9a6a1f; --rust:#9c4a3f;",
              "  --calm:#33566a; --calm-ink:#ffffff; --amber:#9a6a1f; --rust:#9c4a3f;\n  --link:#2f6d9e;", 1)
s = s.replace("    --calm:#8bb0c2; --calm-ink:#101619; --amber:#cfa25c; --rust:#cf887c;",
              "    --calm:#8bb0c2; --calm-ink:#101619; --amber:#cfa25c; --rust:#cf887c;\n    --link:#7fb3d9;", 1)

# ---------- 2. 样式 ----------
CSS = """
/* 系数说明：蓝字链接 + 展开块 */
.helplink{
  display:inline-block; margin-top:10px; padding:0; border:0; background:none;
  font:inherit; font-size:12.5px; color:var(--link);
  border-bottom:1px dashed currentColor; cursor:pointer;
}
.helpbox{
  margin-top:10px; padding:12px 14px; border-radius:12px;
  background:var(--chip); font-size:12.5px; line-height:1.95; color:var(--ink2);
}
.helpbox[hidden]{display:none;}
.helpbox p{margin:0 0 9px;}
.helpbox p:last-child{margin-bottom:0;}
.helpbox b{color:var(--ink); font-weight:600;}

/* 系数计算器 */"""
s = s.replace("\n/* 系数计算器 */", CSS, 1)

# ---------- 3. 帮助块生成函数 ----------
FUNC = """
/* 系数是什么意思：两处计算入口共用 */
function coefHelpHTML(id){
  return '' +
    '<button class="helplink" data-act="toggle-help" data-target="' + id + '">系数是什么意思？</button>' +
    '<div class="helpbox" id="' + id + '" hidden>' +
      '<p>把「系数」当成一个倍率，不是分数，也不是评价。</p>' +
      '<p><b>系数 2</b> 的意思是：这一项每过 1 分钟，扣 2 点。登记 60 分钟就是 120 点。</p>' +
      '<p>不同的事每分钟扣得不一样：刷手机 30 分钟和散步 30 分钟，占的不是同一份额度，所以给不同的倍率。</p>' +
      '<p>系数是反推出来的：<b>系数 = 阈值 ÷ 整天只做这一项的分钟数</b>。阈值 200、你填 100 分钟 → 系数 2；填 400 分钟 → 系数 0.5。</p>' +
      '<p>它回答的是「同样一小时，这件事到底有多耗」。所以别人的系数对你没意义——它取决于你自己的阈值。</p>' +
      '<p>白名单（睡眠、散步这类）不参与计算，系数填什么都无所谓。</p>' +
    '</div>';
}
function sheetCalc(){"""
s = s.replace("\nfunction sheetCalc(){", FUNC, 1)

# ---------- 4. 计算器里挂上去 ----------
OLD = """      '「等效分钟」= 假定整天只做这一项，多少分钟后这条线见底。留空 = 这一项不改。' +
    '</div>' +"""
NEW = """      '「等效分钟」= 假定整天只做这一项，多少分钟后这条线见底。留空 = 这一项不改。' +
    '</div>' +
    coefHelpHTML('calcHelpBox') +"""
assert OLD in s
s = s.replace(OLD, NEW, 1)

# ---------- 5. 新增事件弹层里也挂一个 ----------
OLD2 = """    '<div class="calc-rule">「整天只做这一项，多少分钟后这条线见底」——' +
      '系数 = 阈值 ÷ 这个分钟数（当前精力 ' + fmtPts(eT) + ' / 注意力 ' + fmtPts(aT) + '）。</div>' +
    coefPart +"""
NEW2 = """    '<div class="calc-rule">「整天只做这一项，多少分钟后这条线见底」——' +
      '系数 = 阈值 ÷ 这个分钟数（当前精力 ' + fmtPts(eT) + ' / 注意力 ' + fmtPts(aT) + '）。</div>' +
    coefHelpHTML('npHelpBox') +
    coefPart +"""
assert OLD2 in s
s = s.replace(OLD2, NEW2, 1)

# ---------- 6. 点击展开 / 收起 ----------
OLD3 = """  if (act === 'np-col'){"""
NEW3 = """  if (act === 'toggle-help'){
    var hb = document.getElementById(el.getAttribute('data-target'));
    if (hb){
      hb.hidden = !hb.hidden;
      el.textContent = tr(hb.hidden ? '系数是什么意思？' : '收起说明');
    }
    return;
  }
  if (act === 'np-col'){"""
assert OLD3 in s
s = s.replace(OLD3, NEW3, 1)

# ---------- 7. 英文 ----------
ADD = """  '系数是什么意思？':'What does the coefficient mean?',
  '收起说明':'Hide',
  '把「系数」当成一个倍率，不是分数，也不是评价。':'Treat the coefficient as a multiplier — not a score, not a judgement.',
  '系数 2':'A coefficient of 2',
  ' 的意思是：这一项每过 1 分钟，扣 2 点。登记 60 分钟就是 120 点。':' means: 2 points are deducted per minute. Logging 60 minutes costs 120 points.',
  '不同的事每分钟扣得不一样：刷手机 30 分钟和散步 30 分钟，占的不是同一份额度，所以给不同的倍率。':'Different things drain at different rates: 30 minutes of scrolling and 30 minutes of walking do not take the same thing out of you, so they get different multipliers.',
  '系数是反推出来的：':'The coefficient is derived backwards:',
  '系数 = 阈值 ÷ 整天只做这一项的分钟数':'coefficient = threshold ÷ minutes until you would be done if today were only this',
  '。阈值 200、你填 100 分钟 → 系数 2；填 400 分钟 → 系数 0.5。':'. Threshold 200, you enter 100 minutes → coefficient 2; enter 400 minutes → 0.5.',
  '它回答的是「同样一小时，这件事到底有多耗」。所以别人的系数对你没意义——它取决于你自己的阈值。':'It answers “for the same hour, how much does this really cost me”. Someone else’s coefficients are meaningless to you — they depend on your own thresholds.',
  '白名单（睡眠、散步这类）不参与计算，系数填什么都无所谓。':'White-list items (sleep, walking) are not counted at all — whatever you put as their coefficient does not matter.',
"""
s = s.replace("  /* 提示语 */", ADD + "\n  /* 提示语 */", 1)

io.open(p, 'w', encoding='utf-8').write(s)
print('patched')

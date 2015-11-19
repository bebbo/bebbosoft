var $closed = new Image(16,16); $closed.src = "gfx/closed.gif"; 
var $selected = new Image(16,16); $selected.src = "gfx/selected.gif";
var $opened = new Image(16,16); $opened.src = "gfx/opened.gif";
var $last = new Image(16,16); $last.src = "gfx/last.gif";
function $m(c, n, t, l, d) {
  if (!l) l = '#';
  var s = "<div class='m'><table><tr><t"
  + (n ? "h" : "d")
  + "><img src='gfx/"
  + (c ? "opened.gif' alt='+' onclick='javascript:$t(this)" : "last.gif' alt='")
  + (n ? "' /></th" : "'/></td")
  + "><td><div class='m'><img src='gfx/drawer.gif' onclick='javascript:$s(this)' alt='D' /><a href='"
  + l + "' onClick='javascript:$s(this)'";
  if (d) s += "target='" + d + "'";
  s += ">" + t  + "</a></div>";
  document.write(s);
}
function $e() { document.write("</td></tr></table></div>"); }
var $last;
function $s(e) {
  if ($last) {
    $last.src = "gfx/drawer.gif";
    $last = $n($last, 'A');
    if ($last) $last.style.fontWeight = "normal";
  }
  var im = e;
  if (e.nodeName == 'IMG') e = $n(e, 'A');
  else im = $d(e.parentNode, 'IMG');
  for(var d = $u(im.parentNode, 'DIV');d;) {
    d = $u(d.parentNode, 'DIV');
    if (!d) break;
    var h1 = d.clientHeight;
    var h2 = d.scrollHeight;
    if (h1 > 0 && h1 < h2) $t($d(d, 'IMG'));
  }
  if (e) {
    $a(e);
    e.style.fontWeight = "bold";
  }
  im.src = $selected.src;
  $last = im;  
}
function $a(o) {
  if (o.nodeName == 'A') {
    if (o.click) {
      o.click();
      return true;
    }
    if (parent[o.target]) parent[o.target].location = o.href;
    else {
      var e = parent.document.getElementById(o.target);
      if (e) e.src = o.href;
    }
    return true;
  }
  for (var i = 0; i < o.childNodes.length; ++i) {
    if ($a(o.childNodes[i]))
      return true;
  }
  return false;
}
function $t(im) {
  if (!im) return;
  if (im.src.substring(im.src.length - 8) == 'last.gif') return;
  var y = window.pageYOffset;
  if (!y) y = document.body.scrollTop;
  var e = $u(im, 'DIV');
  var e1 = $d(im.parentNode.nextSibling, 'DIV');
  if (!e1) return;
  var h = e.clientHeight;
  var h1 = e1.scrollHeight;
  var h2 = e.scrollHeight;
  if (h <= h1 + 1) {
    e.style.height = h2;
    h = h2 - h;
    im.src="gfx/opened.gif";
  } else {
    im.src=$closed.src;
    e.style.height = h1;
    h = h1 - h;
  }  
  var o = e;
  for(o = $u(o.parentNode, 'DIV');o;o = $u(o.parentNode, 'DIV'))
  {
    if (o.style.height) o.style.height = o.clientHeight + h;
    o.style.height = "";
  }
  window.scrollTo(0, y);
}
function $u(n, t) {
  for(;n;n = n.parentNode) { if (n.nodeName == t) return n;}
}
function $n(n, t) {
  for(;n;n = n.nextSibling) { if (n.nodeName == t) return n;}
}
function $d(n, t) {
  for(;n;n = n.firstChild) {
    if (n.nodeName == '#text') n = n.nextSibling;
    if (!n) return;
    if (n.nodeName == t) return n;
  }
}
function $fa(o,n) {
    if (o.nodeName == 'A' && o.href.indexOf(n) >= 0) return o;      
    for (var i = 0; i < o.childNodes.length; ++i) {
      var r = $fa(o.childNodes[i], n);
      if (r) return r;
    }
  return null;
}
function $f(i) {
  var div = $n($d(document.body, 'DIV').firstChild, 'DIV');
  var cd = $fa(div, i);
  if (cd) {
    var im = $d(cd.parentNode, 'IMG');
    im = $u($u(im, 'DIV'), 'DIV');
    im = $d(im, 'IMG')
    $s(im);
  }
}
function $r(d) {
  do {
    var im = $d(d,'IMG');
    var cd = im.parentNode.nextSibling.firstChild.nextSibling;
    if (cd) $r(cd);
    $t(im);
    d = $n(d.nextSibling, 'DIV');
  } while(d);
}
function $z() {
  if (!$last.complete || !$opened.complete || !$selected.complete ||! $closed.complete) {
    setTimeout("$z()", 5); // avoid hang and retry asap
    return;
  } 
  var div = $n($d(document.body, 'DIV').firstChild, 'DIV');
  $r(div);
}

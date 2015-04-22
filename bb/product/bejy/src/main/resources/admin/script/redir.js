// (c) by Stefan Bebbo Franke 1998-2005
  var BASIS = '/admin'; 

  function bm_mark(target)
  {
    var nav = parent.frames['navi'];
    var ls = nav.document.links;
    for (var i = 0; i < ls.length; ++i) {
//        alert('' + ls[i].href + "=="  + '' + target);
      if ('' + ls[i].href == '' + target) {
//        alert('' + ls[i].href + "=="  + '' + target);
        var id = ls[i].parentNode.id;
        id = id.substring(0, id.length - 2) + 'ico';
        var img = nav.document.images[id];
        if (img && img.onclick) img.onclick(img);
        break;
      }
    }
  }

  function _handleA(ev)
  {
    if (!ev) ev = window.event;
    if (!ev.target) ev.target = ev.srcElement;
    if (ev.target.nodeName == "A" && ev.target.href) bm_mark(ev.target.href);
  }

  function _handleF(ev)
  {
    if (!ev) ev = window.event;
    if (!ev.target) ev.target = ev.srcElement;
    if (ev.target.action) bm_mark(ev.target.action);
  }

  function hookme()
  {
    if (document.all)
    {
      document.onclick = _handleA;
      var fs = document.forms;
      for (var i = 0; i < fs.length; ++i) {
        fs[i].onsubmit = _handleF;
      }
    } else {
      document.addEventListener("click", _handleA, true);
      document.addEventListener("submit", _handleF, true);
    }
  }

  function reframe() {
    var u = '' + this.location;
    if(parent.frames.length == 0) {
      var i = u.indexOf(BASIS);
      i = u.indexOf('/', i + 1) + 1;
      this.document.location = u.substring(0, i) + '#' + u.substring(i);
      return;
    }
	bm_mark(u);    
  }

  function checkMain() {
    var t = '' + parent.location;
    var ri = t.indexOf("#");
    if (ri > 0) {
      var s = t.substring(0, ri);
      var u = s + t.substring(ri + 1);
      var l = '' + parent.main.location;
      if (l != u) {
        parent.main.location = u;
      }
    }
  }

  function force() {
    var f = parent.frames;
    for (var i = 0; i < f.length; ++i)
    {
      if (f[i] != this) {
        f[i].location = '' + f[i].location;
      }
    }
  }
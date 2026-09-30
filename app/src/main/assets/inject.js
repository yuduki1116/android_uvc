(function () {
  if (window.__uvcInjected) return; window.__uvcInjected = true;
  const md = navigator.mediaDevices;
  const orig = md.getUserMedia.bind(md);
  const canvas = document.createElement("canvas");
  canvas.width = 640; canvas.height = 480;
  const ctx = canvas.getContext("2d");
  const img = new Image();
  let started = false;
  function start() {
    if (started) return; started = true;
    img.onload = () => {
      if (canvas.width !== img.width) { canvas.width = img.width; canvas.height = img.height; }
      ctx.drawImage(img, 0, 0);
    };
    setInterval(() => {
      try { const b = window.UVC.frame(); if (b) img.src = "data:image/jpeg;base64," + b; } catch (e) {}
    }, 66);
  }
  md.getUserMedia = async function (c) {
    if (c && c.video && window.UVC && window.UVC.available()) {
      start();
      const stream = canvas.captureStream(15);
      if (c.audio) {
        try { (await orig({ audio: c.audio })).getAudioTracks().forEach(t => stream.addTrack(t)); } catch (e) {}
      }
      return stream;
    }
    return orig(c);
  };
})();

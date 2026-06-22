/**
 * MoMo Bridge — Payment Verification Widget
 *
 * Design: Cold, precise financial terminal. Dark ground (#0A0E1A),
 * gold (#D4A843) single accent. Mechanical, not consumer.
 *
 * Usage:
 *   <script src="https://momo-bridge.vercel.app/momobridge.js"></script>
 *   <script>
 *     MoMoBridge.popup({
 *       relayUrl: 'https://your-relay.com',
 *       apiKey: 'mb_...',
 *       onSuccess: function(data) { console.log('Confirmed', data); },
 *       onFailure: function(data) { console.log('Failed', data); },
 *     });
 *   </script>
 *
 * The amount is returned by the relay in data.transaction.amount
 * and displayed in the result card — no amount input needed.
 *
 * Modes: popup, inline, redirect
 */
(function () {
  'use strict';

  var VERSION = '1.2.0';
  var DEFAULT_CURRENCY = 'GH\u20B5';
  var STYLES_ID = '__momobridge_styles';
  var FONTS_LOADED = false;

  // ─── Font Loader ──────────────────────────────────────────────────────────

  function loadFonts(callback) {
    if (FONTS_LOADED) { if (callback) callback(); return; }
    var link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href =
      'https://fonts.googleapis.com/css2?' +
      'family=Space+Grotesk:wght@400;500;600;700&' +
      'family=JetBrains+Mono:wght@400;500;700&' +
      'family=Inter:wght@400;500;600&' +
      'display=swap';
    link.onload = function () {
      FONTS_LOADED = true;
      if (callback) callback();
    };
    link.onerror = function () {
      FONTS_LOADED = true;
      if (callback) callback();
    };
    document.head.appendChild(link);
  }

  // ─── Design tokens (injected once) ────────────────────────────────────────

  function injectStyles() {
    if (document.getElementById(STYLES_ID)) return;

    var css = (
      '#__mb-overlay{' +
        'position:fixed;inset:0;z-index:2147483647;' +
        'display:flex;align-items:center;justify-content:center;' +
        'background:rgba(10,14,26,0.82);' +
        'font-family:Inter,system-ui,-apple-system,sans-serif;' +
        'color:#E8EDF5;' +
        'line-height:1.5;' +
        '-webkit-font-smoothing:antialiased;' +
      '}' +
      '#__mb-modal{' +
        'width:400px;max-width:92vw;' +
        'background:#0F1424;' +
        'border:1px solid #1E2748;' +
        'border-radius:12px;' +
        'box-shadow:0 24px 80px -12px rgba(0,0,0,0.5),0 8px 24px -8px rgba(0,0,0,0.3);' +
        'overflow:hidden;' +
        'animation:__mb_modalIn 0.25s cubic-bezier(0.19,1,0.22,1) both;' +
      '}' +
      '@keyframes __mb_modalIn{' +
        'from{opacity:0;transform:scale(0.96) translateY(12px)}' +
        'to{opacity:1;transform:scale(1) translateY(0)}' +
      '}' +
      '@keyframes __mb_fadeIn{' +
        'from{opacity:0;transform:translateY(8px)}' +
        'to{opacity:1;transform:translateY(0)}' +
      '}' +
      '@keyframes __mb_spin{' +
        'to{transform:rotate(360deg)}' +
      '}' +
      '#__mb-header{' +
        'display:flex;align-items:center;justify-content:space-between;' +
        'padding:16px 20px;' +
        'border-bottom:1px solid #1E2748;' +
      '}' +
      '#__mb-wordmark{' +
        'font-family:"Space Grotesk",system-ui,sans-serif;' +
        'font-size:16px;font-weight:700;' +
        'letter-spacing:-0.02em;' +
        'color:#D4A843;' +
      '}' +
      '#__mb-close{' +
        'display:flex;align-items:center;justify-content:center;' +
        'width:32px;height:32px;' +
        'background:none;border:1px solid #1E2748;border-radius:8px;' +
        'cursor:pointer;' +
        'color:#5A6480;font-size:18px;line-height:1;padding:0;' +
        'transition:all 0.15s ease;' +
      '}' +
      '#__mb-close:hover{' +
        'color:#E8EDF5;border-color:#D4A843;' +
      '}' +
      '#__mb-body{' +
        'padding:24px 20px 20px;' +
      '}' +

      '#__mb-field{' +
        'margin-bottom:16px;' +
      '}' +
      '#__mb-field label{' +
        'display:block;' +
        'font-size:12px;font-weight:500;' +
        'color:#8B95B0;margin-bottom:6px;' +
        'letter-spacing:0.02em;' +
      '}' +
      '#__mb-ref-input{' +
        'display:block;width:100%;padding:10px 14px;' +
        'background:#0A0E1A;' +
        'border:1px solid #1E2748;border-radius:10px;' +
        'color:#E8EDF5;font-family:"JetBrains Mono",monospace;' +
        'font-size:14px;outline:none;box-sizing:border-box;' +
        'transition:border-color 0.15s ease;' +
      '}' +
      '#__mb-ref-input:focus{' +
        'border-color:#D4A843;' +
        'box-shadow:0 0 0 3px rgba(212,168,67,0.12);' +
      '}' +
      '#__mb-ref-input::placeholder{' +
        'color:#5A6480;' +
      '}' +
      '#__mb-ref-input.__mb-error{' +
        'border-color:#EF5350;' +
        'box-shadow:0 0 0 3px rgba(239,83,80,0.12);' +
      '}' +
      '#__mb-ref-input:disabled{' +
        'opacity:0.5;cursor:not-allowed;' +
      '}' +
      '#__mb-error-text{' +
        'font-size:11px;color:#EF5350;margin-top:4px;' +
        'display:none;' +
      '}' +
      '#__mb-btn{' +
        'display:flex;align-items:center;justify-content:center;gap:8px;' +
        'width:100%;height:48px;padding:0 24px;' +
        'background:#D4A843;color:#0A0E1A;border:none;border-radius:20px;' +
        'font-family:Inter,system-ui,sans-serif;' +
        'font-size:14px;font-weight:600;' +
        'letter-spacing:0.01em;' +
        'cursor:pointer;' +
        'transition:all 0.15s ease;' +
        'box-sizing:border-box;' +
      '}' +
      '#__mb-btn:hover:not(:disabled){' +
        'background:#EBC875;' +
      '}' +
      '#__mb-btn:active:not(:disabled){' +
        'transform:scale(0.97);' +
      '}' +
      '#__mb-btn:disabled{' +
        'opacity:0.5;cursor:not-allowed;' +
      '}' +
      '#__mb-btn .__mb-spinner{' +
        'width:16px;height:16px;' +
        'border:2px solid rgba(10,14,26,0.25);' +
        'border-top-color:#0A0E1A;' +
        'border-radius:50%;' +
        'animation:__mb_spin 0.6s linear infinite;' +
        'flex-shrink:0;' +
      '}' +
      '#__mb-result{' +
        'margin-top:16px;min-height:0;' +
      '}' +
      '.__mb-result-card{' +
        'padding:20px;border-radius:12px;' +
        'text-align:center;' +
        'animation:__mb_fadeIn 0.2s cubic-bezier(0.19,1,0.22,1) both;' +
      '}' +
      '.__mb-result-success{' +
        'background:rgba(0,200,83,0.08);' +
        'border:1px solid rgba(0,200,83,0.25);' +
      '}' +
      '.__mb-result-error{' +
        'background:rgba(239,83,80,0.08);' +
        'border:1px solid rgba(239,83,80,0.25);' +
      '}' +
      '.__mb-result-warn{' +
        'background:rgba(240,180,41,0.08);' +
        'border:1px solid rgba(240,180,41,0.25);' +
      '}' +
      '.__mb-result-icon{' +
        'width:40px;height:40px;border-radius:50%;' +
        'display:flex;align-items:center;justify-content:center;' +
        'margin:0 auto 12px;' +
        'font-size:18px;font-weight:700;' +
      '}' +
      '.__mb-result-success .__mb-result-icon{' +
        'background:rgba(0,200,83,0.15);color:#00C853;' +
      '}' +
      '.__mb-result-error .__mb-result-icon{' +
        'background:rgba(239,83,80,0.15);color:#EF5350;' +
      '}' +
      '.__mb-result-warn .__mb-result-icon{' +
        'background:rgba(240,180,41,0.15);color:#F0B429;' +
      '}' +
      '.__mb-result-title{' +
        'font-family:"Space Grotesk",system-ui,sans-serif;' +
        'font-size:15px;font-weight:700;margin-bottom:2px;' +
      '}' +
      '.__mb-result-success .__mb-result-title{color:#00C853;}' +
      '.__mb-result-error .__mb-result-title{color:#EF5350;}' +
      '.__mb-result-warn .__mb-result-title{color:#F0B429;}' +
      '.__mb-result-sender{' +
        'font-size:13px;color:#8B95B0;margin:2px 0 6px;' +
      '}' +
      '.__mb-result-amount{' +
        'font-family:"JetBrains Mono",monospace;' +
        'font-size:20px;font-weight:700;' +
        'color:#E8EDF5;margin:6px 0;' +
      '}' +
      '.__mb-result-ref{' +
        'display:inline-block;' +
        'font-family:"JetBrains Mono",monospace;' +
        'font-size:11px;color:#5A6480;' +
        'background:#0A0E1A;padding:4px 10px;border-radius:6px;' +
        'margin-top:4px;' +
      '}' +
      '.__mb-result-message{' +
        'font-size:13px;color:#8B95B0;margin-top:8px;line-height:1.5;' +
      '}' +
      '.__mb-result-retry{' +
        'margin-top:14px;padding:8px 24px;' +
        'background:transparent;color:#D4A843;' +
        'border:1px solid #D4A843;border-radius:20px;' +
        'font-family:Inter,system-ui,sans-serif;' +
        'font-size:12px;font-weight:600;cursor:pointer;' +
        'transition:all 0.15s ease;' +
      '}' +
      '.__mb-result-retry:hover{' +
        'background:rgba(212,168,67,0.1);' +
      '}' +
      '#__mb-hint{' +
        'margin-top:12px;padding:10px 14px;border-radius:8px;' +
        'font-size:12px;line-height:1.5;color:#5A6480;' +
        'background:rgba(212,168,67,0.06);' +
        'border:1px solid rgba(212,168,67,0.08);' +
      '}' +
      '#__mb-inline-container .__mb-widget-wrapper{' +
        'margin:0;' +
      '}' +
      '@media(prefers-reduced-motion:reduce){' +
        '#__mb-modal{animation:none}' +
        '.__mb-result-card{animation:none}' +
        '#__mb-btn .__mb-spinner{animation:none}' +
      '}'
    );

    var style = document.createElement('style');
    style.id = STYLES_ID;
    style.textContent = css;
    document.head.appendChild(style);
  }

  // ─── Core Verification ──────────────────────────────────────────────────────

  function verify(relayUrl, apiKey, ref, onSuccess, onFailure) {
    var url = relayUrl.replace(/\/+$/, '') + '/claim';
    var xhr = new XMLHttpRequest();
    xhr.open('POST', url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onreadystatechange = function () {
      if (xhr.readyState !== 4) return;
      try {
        var data = JSON.parse(xhr.responseText);
        if (data.confirmed) {
          if (onSuccess) onSuccess(data);
        } else {
          if (onFailure) onFailure(data);
        }
      } catch (e) {
        if (onFailure) onFailure({ message: 'Invalid response from relay' });
      }
    };
    xhr.onerror = function () {
      if (onFailure) onFailure({ message: 'Network error \u2014 could not reach relay' });
    };
    xhr.send(JSON.stringify({ apiKey: apiKey, reference: ref }));
  }

  function notifyParent(data) {
    try {
      var txn = data.transaction || {};
      window.parent.postMessage(
        {
          type: 'momo_result',
          confirmed: data.confirmed,
          reference: txn.reference || data.reference || '',
          amount: txn.amount,
          senderName: txn.senderName || null,
          transaction: txn,
        },
        '*'
      );
    } catch (e) {}
  }

  function friendlyMessage(data) {
    var m = (data.message || '').toLowerCase();
    if (data.confirmed) return 'Payment verified by your phone.';
    if (m.indexOf('already') !== -1) return 'This payment was already confirmed.';
    if (m.indexOf('invalid') !== -1) return 'Reference not found in recent transactions.';
    if (m.indexOf('amount') !== -1) return 'The amount doesn\u2019t match what was sent.';
    if (m.indexOf('expired') !== -1) return 'This reference has expired.';
    if (m.indexOf('offline') !== -1) return 'The store phone is currently offline.';
    if (m.indexOf('network') !== -1) return 'Could not reach the verification server.';
    return m || 'Verification failed.';
  }

  // ─── Result Card Builder ─────────────────────────────────────────────────────

  var ICONS = {
    success: '\u2713',
    error: '\u2717',
    warn: '\u26A0',
  };

  function createEl(tag, attrs, children) {
    var el = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (k) {
        if (k === 'style' && typeof attrs[k] === 'object') {
          Object.assign(el.style, attrs[k]);
        } else if (k === 'className') {
          el.className = attrs[k];
        } else {
          el.setAttribute(k, attrs[k]);
        }
      });
    }
    if (children) {
      children.forEach(function (c) {
        if (typeof c === 'string') {
          el.appendChild(document.createTextNode(c));
        } else if (c) {
          el.appendChild(c);
        }
      });
    }
    return el;
  }

  function isElement(el) {
    return typeof el === 'object' && el !== null && el.nodeType === 1;
  }

  function queryEl(selector) {
    if (isElement(selector)) return selector;
    if (typeof selector === 'string') return document.querySelector(selector);
    return null;
  }

  function buildResultCard(type, opts) {
    var icon = ICONS[type] || '';
    var card = createEl('div', { className: '__mb-result-card __mb-result-' + type });

    var iconEl = createEl('div', { className: '__mb-result-icon' }, [icon]);
    card.appendChild(iconEl);

    if (opts.title) {
      card.appendChild(createEl('div', { className: '__mb-result-title' }, [opts.title]));
    }
    if (opts.senderName) {
      card.appendChild(createEl('div', { className: '__mb-result-sender' }, [opts.senderName]));
    }
    if (opts.amount != null) {
      var sym = opts.currencySymbol || DEFAULT_CURRENCY;
      card.appendChild(
        createEl('div', { className: '__mb-result-amount' }, [sym + ' ' + opts.amount])
      );
    }
    if (opts.reference) {
      card.appendChild(
        createEl('div', { className: '__mb-result-ref' }, ['Ref: ' + opts.reference])
      );
    }
    if (opts.message) {
      card.appendChild(
        createEl('div', { className: '__mb-result-message' }, [opts.message])
      );
    }
    if (opts.retry) {
      var retryBtn = createEl('button', { className: '__mb-result-retry' }, ['Try Again']);
      retryBtn.addEventListener('click', opts.retry);
      card.appendChild(retryBtn);
    }
    return card;
  }

  // ─── Widget UI Builder ───────────────────────────────────────────────────────

  function buildWidget(options, container) {
    var relayUrl = options.relayUrl;
    var apiKey = options.apiKey;
    var currencySymbol = options.currencySymbol || DEFAULT_CURRENCY;
    var onSuccess = options.onSuccess || function () {};
    var onFailure = options.onFailure || function () {};

    var refInput, resultEl, hintEl, errorTextEl, btn, btnTextSpan;

    function showResult(type, opts) {
      btn.disabled = false;
      btn.innerHTML = '';
      btn.appendChild(btnTextSpan);
      resultEl.innerHTML = '';
      opts = opts || {};
      opts.currencySymbol = currencySymbol;
      resultEl.appendChild(buildResultCard(type, opts));
    }

    function setLoading() {
      btn.disabled = true;
      btn.innerHTML = '<span class="__mb-spinner"></span> Verifying\u2026';
      resultEl.innerHTML = '';
    }

    function handleVerify() {
      var ref = refInput.value.trim();
      if (!ref) {
        refInput.className = '__mb-error';
        errorTextEl.style.display = 'block';
        refInput.focus();
        return;
      }
      refInput.className = '';
      errorTextEl.style.display = 'none';
      setLoading();

      verify(
        relayUrl,
        apiKey,
        ref,
        function (data) {
          var txn = data.transaction || {};
          var actualAmount = txn.amount;
          var senderName = txn.senderName || null;
          var msg = 'Payment confirmed by the store phone.';
          showResult('success', {
            title: 'Payment Confirmed',
            senderName: senderName ? 'from ' + senderName : null,
            amount: actualAmount != null ? actualAmount.toFixed(2) : null,
            reference: ref,
            message: msg,
          });
          notifyParent(data);
          onSuccess(data);
        },
        function (data) {
          var msg = friendlyMessage(data);
          var lower = msg.toLowerCase();
          var type = 'error';
          var title = 'Verification Failed';
          if (lower.indexOf('already') !== -1) {
            type = 'warn';
            title = 'Already Confirmed';
          }
          showResult(type, {
            title: title,
            message: msg,
            retry: function () { resultEl.innerHTML = ''; },
          });
          notifyParent(data);
          onFailure(data);
        }
      );
    }

    // ─── Build DOM ─────────────────────────────────────────────────────────────

    // Reference field
    var field = createEl('div', { id: '__mb-field' });
    var label = createEl('label', {}, ['Transaction Reference']);
    refInput = createEl('input', {
      id: '__mb-ref-input',
      type: 'text',
      placeholder: 'e.g. 0000013331054115',
      value: options.reference || '',
    });
    refInput.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') handleVerify();
    });
    refInput.addEventListener('input', function () {
      refInput.className = '';
      errorTextEl.style.display = 'none';
    });

    errorTextEl = createEl('div', { id: '__mb-error-text' }, ['Please enter the transaction reference.']);

    field.appendChild(label);
    field.appendChild(refInput);
    field.appendChild(errorTextEl);
    container.appendChild(field);

    // Verify button
    btn = createEl('button', { id: '__mb-btn' });
    btnTextSpan = document.createTextNode('Verify Payment');
    btn.appendChild(btnTextSpan);
    btn.addEventListener('click', handleVerify);
    container.appendChild(btn);

    // Result area
    resultEl = createEl('div', { id: '__mb-result' });
    container.appendChild(resultEl);

    // Hint
    hintEl = createEl('div', { id: '__mb-hint' }, [
      document.createTextNode(
        'Send the exact amount via MoMo to the store phone, then enter the SMS reference above.'
      ),
    ]);
    container.appendChild(hintEl);
  }

  // ─── Popup Mode ─────────────────────────────────────────────────────────────

  function popup(options) {
    if (!options || !options.relayUrl || !options.apiKey) {
      console.error('[MoMoBridge] relayUrl and apiKey are required');
      return;
    }

    injectStyles();
    loadFonts();

    var overlay = createEl('div', { id: '__mb-overlay' });
    var modal = createEl('div', { id: '__mb-modal' });

    // Header
    var closeBtn = createEl('button', { id: '__mb-close' }, ['\u00D7']);
    var header = createEl('div', { id: '__mb-header' }, [
      createEl('span', { id: '__mb-wordmark' }, [options.title || 'MoMo Bridge']),
      closeBtn,
    ]);

    // Body
    var body = createEl('div', { id: '__mb-body' });
    buildWidget(options, body);

    modal.appendChild(header);
    modal.appendChild(body);
    overlay.appendChild(modal);
    document.body.appendChild(overlay);

    function close() {
      if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
      if (options.onClose) options.onClose();
    }

    closeBtn.addEventListener('click', close);
    overlay.addEventListener('click', function (e) {
      if (e.target === overlay) close();
    });
    document.addEventListener('keydown', function esc(e) {
      if (e.key === 'Escape') { close(); document.removeEventListener('keydown', esc); }
    });
  }

  // ─── Inline Mode ────────────────────────────────────────────────────────────

  function inline(options) {
    if (!options || !options.relayUrl || !options.apiKey) {
      console.error('[MoMoBridge] relayUrl and apiKey are required');
      return;
    }
    var container = queryEl(options.container);
    if (!container) {
      console.error('[MoMoBridge] container not found:', options.container);
      return;
    }

    injectStyles();
    loadFonts();

    var wrapper = createEl('div', {
      className: '__mb-widget-wrapper',
      style: { maxWidth: '400px', margin: '0 auto' },
    });
    container.appendChild(wrapper);
    buildWidget(options, wrapper);
  }

  // ─── Redirect Mode ──────────────────────────────────────────────────────────

  function redirect(options) {
    if (!options || !options.relayUrl || !options.apiKey) {
      console.error('[MoMoBridge] relayUrl and apiKey are required');
      return;
    }
    var base = options.widgetUrl || 'https://momo-bridge.vercel.app/widget.html';
    var params =
      '?relayUrl=' + encodeURIComponent(options.relayUrl) +
      '&apiKey=' + encodeURIComponent(options.apiKey) +
      '&currencySymbol=' + encodeURIComponent(options.currencySymbol || DEFAULT_CURRENCY);
    if (options.reference) params += '&reference=' + encodeURIComponent(options.reference);
    if (options.callbackUrl) params += '&callbackUrl=' + encodeURIComponent(options.callbackUrl);

    if (options.target === 'self') {
      window.location.href = base + params;
    } else {
      window.open(base + params, '_blank', 'width=480,height=640');
    }
  }

  // ─── Expose Global ──────────────────────────────────────────────────────────

  var MoMoBridge = {
    VERSION: VERSION,
    popup: popup,
    inline: inline,
    redirect: redirect,
  };

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = MoMoBridge;
  } else if (typeof define === 'function' && define.amd) {
    define([], function () { return MoMoBridge; });
  } else {
    window.MoMoBridge = MoMoBridge;
  }
})();

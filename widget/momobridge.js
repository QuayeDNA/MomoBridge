/**
 * MoMo Bridge — Embeddable Payment Verification Widget
 *
 * Usage:
 *   <script src="https://momo-bridge.vercel.app/momobridge.js"></script>
 *   <script>
 *     MoMoBridge.popup({
 *       relayUrl: 'https://your-relay.com',
 *       apiKey: 'mb_...',
 *       amount: 18.00,
 *       currencySymbol: 'GH\u20B5',
 *       onSuccess: function(data) { console.log('Confirmed', data); },
 *       onFailure: function(data) { console.log('Failed', data); },
 *     });
 *   </script>
 *
 * Modes: popup, inline, redirect
 */
(function () {
  'use strict';

  var VERSION = '1.1.0';

  var DEFAULT_CURRENCY = 'GH\u20B5';

  // ─── Utilities ──────────────────────────────────────────────────────────────

  function isElement(el) {
    return typeof el === 'object' && el !== null && el.nodeType === 1;
  }

  function queryEl(selector) {
    if (isElement(selector)) return selector;
    if (typeof selector === 'string') return document.querySelector(selector);
    return null;
  }

  function createEl(tag, attrs, children) {
    var el = document.createElement(tag);
    if (attrs)
      Object.keys(attrs).forEach(function (k) {
        if (k === 'style' && typeof attrs[k] === 'object')
          Object.assign(el.style, attrs[k]);
        else if (k === 'className') el.className = attrs[k];
        else el.setAttribute(k, attrs[k]);
      });
    if (children)
      children.forEach(function (c) {
        if (typeof c === 'string') el.appendChild(document.createTextNode(c));
        else if (c) el.appendChild(c);
      });
    return el;
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

  // ─── Styles ─────────────────────────────────────────────────────────────────

  var STYLES_ID = '__momobridge_styles';

  function injectStyles() {
    if (document.getElementById(STYLES_ID)) return;
    var css =
      '#__mb-overlay {\
        position:fixed;inset:0;z-index:2147483647;\
        display:flex;align-items:center;justify-content:center;\
        background:rgba(10,14,26,0.7);\
        font-family:-apple-system,system-ui,sans-serif;\
      }\
      #__mb-modal {\
        width:400px;max-width:92vw;\
        background:#1a1a2e;border:1px solid #2a2a4e;border-radius:16px;\
        box-shadow:0 20px 60px rgba(0,0,0,0.5);\
        overflow:hidden;\
        animation:__mb_fadeIn 0.2s ease-out;\
      }\
      @keyframes __mb_fadeIn {\
        from { opacity:0; transform:scale(0.95) translateY(8px); }\
        to { opacity:1; transform:scale(1) translateY(0); }\
      }\
      #__mb-header {\
        display:flex;align-items:center;justify-content:space-between;\
        padding:16px 20px;\
        border-bottom:1px solid #2a2a4e;\
      }\
      #__mb-header h3 {\
        margin:0;font-size:16px;font-weight:700;color:#d4a843;\
      }\
      #__mb-close {\
        background:none;border:none;cursor:pointer;\
        color:#5a6480;font-size:20px;line-height:1;padding:4px;\
      }\
      #__mb-close:hover { color:#e8edf5; }\
      #__mb-body { padding:20px; }\
      #__mb-body label {\
        display:block;font-size:13px;color:#8b95b0;margin-bottom:4px;\
      }\
      #__mb-body input {\
        width:100%;padding:10px 12px;margin-bottom:12px;\
        background:#0f1424;border:1px solid #1e2748;border-radius:10px;\
        color:#e8edf5;font-size:14px;outline:none;box-sizing:border-box;\
      }\
      #__mb-body input:focus { border-color:#d4a843; }\
      #__mb-body input:disabled { opacity:0.5; }\
      #__mb-body input.__mb-error-input { border-color:#ef5350; }\
      #__mb-amount-display {\
        display:flex;justify-content:space-between;align-items:center;\
        padding:10px 12px;margin-bottom:12px;\
        background:#0f1424;border:1px solid #1e2748;border-radius:10px;\
      }\
      #__mb-amount-display span:first-child { color:#8b95b0;font-size:13px; }\
      #__mb-amount-display span:last-child { color:#00c853;font-weight:700;font-size:15px; }\
      #__mb-btn {\
        width:100%;padding:12px;margin-top:4px;\
        background:#d4a843;color:#0a0e1a;border:none;border-radius:20px;\
        font-size:14px;font-weight:600;cursor:pointer;\
        transition:opacity 0.15s;\
      }\
      #__mb-btn:hover { opacity:0.9; }\
      #__mb-btn:disabled { opacity:0.5;cursor:not-allowed; }\
      #__mb-btn:active { transform:scale(0.97); }\
      .__mb-result-card {\
        margin-top:12px;padding:16px;border-radius:12px;\
        text-align:center;animation:__mb_fadeIn 0.2s ease-out;\
      }\
      .__mb-result-success { background:rgba(0,200,83,0.1);border:1px solid rgba(0,200,83,0.3); }\
      .__mb-result-error { background:rgba(239,83,80,0.1);border:1px solid rgba(239,83,80,0.3); }\
      .__mb-result-warn { background:rgba(240,180,41,0.1);border:1px solid rgba(240,180,41,0.3); }\
      .__mb-result-icon {\
        width:40px;height:40px;border-radius:50%;\
        display:flex;align-items:center;justify-content:center;\
        margin:0 auto 10px;font-size:18px;font-weight:700;\
      }\
      .__mb-result-success .__mb-result-icon { background:rgba(0,200,83,0.2);color:#00c853; }\
      .__mb-result-error .__mb-result-icon { background:rgba(239,83,80,0.2);color:#ef5350; }\
      .__mb-result-warn .__mb-result-icon { background:rgba(240,180,41,0.2);color:#f0b429; }\
      .__mb-result-title {\
        font-size:15px;font-weight:700;margin-bottom:4px;\
      }\
      .__mb-result-success .__mb-result-title { color:#00c853; }\
      .__mb-result-error .__mb-result-title { color:#ef5350; }\
      .__mb-result-warn .__mb-result-title { color:#f0b429; }\
      .__mb-result-amount {\
        font-size:20px;font-weight:700;color:#e8edf5;margin:6px 0;\
      }\
      .__mb-result-ref {\
        font-size:11px;color:#5a6480;font-family:monospace;\
        background:#0f1424;padding:4px 8px;border-radius:4px;\
        display:inline-block;margin-top:4px;\
      }\
      .__mb-result-message {\
        font-size:13px;color:#8b95b0;margin-top:4px;line-height:1.4;\
      }\
      .__mb-result-retry {\
        margin-top:12px;padding:8px 20px;\
        background:transparent;color:#d4a843;border:1px solid #d4a843;\
        border-radius:20px;font-size:12px;font-weight:600;cursor:pointer;\
      }\
      .__mb-result-retry:hover { background:rgba(212,168,67,0.1); }\
      .__mb-loading {\
        display:flex;align-items:center;justify-content:center;gap:8px;\
        padding:8px 0;color:#8b95b0;font-size:13px;\
      }\
      .__mb-spinner {\
        width:16px;height:16px;border:2px solid #2a2a4e;\
        border-top-color:#d4a843;border-radius:50%;\
        animation:__mb_spin 0.6s linear infinite;\
      }\
      @keyframes __mb_spin { to { transform:rotate(360deg); } }\
      .__mb-hint {\
        margin-top:8px;padding:8px 12px;border-radius:8px;\
        font-size:12px;color:#5a6480;\
        background:rgba(212,168,67,0.08);\
      }\
      #__mb-inline-container .__mb-widget-wrapper { margin:0; }\
      .__mb-hidden { display:none !important; }';
    var style = document.createElement('style');
    style.id = STYLES_ID;
    style.textContent = css;
    document.head.appendChild(style);
  }

  // ─── Core Verification ──────────────────────────────────────────────────────

  function verify(relayUrl, apiKey, ref, amount, onSuccess, onFailure) {
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
    xhr.send(JSON.stringify({ apiKey: apiKey, reference: ref, amount: amount }));
  }

  function notifyParent(data) {
    try {
      var txn = data.transaction || {};
      window.parent.postMessage(
        {
          type: 'momo_result',
          confirmed: data.confirmed,
          reference: data.reference || txn.reference || '',
          amount: data.amount,
          senderName: txn.senderName || null,
          transaction: txn,
        },
        '*'
      );
    } catch (e) {}
  }

  // ─── Result Card Builder ─────────────────────────────────────────────────────

  function buildResultCard(type, opts) {
    // type: 'success' | 'error' | 'warn'
    var iconMap = { success: '\u2713', error: '\u2717', warn: '\u26A0' };
    var icon = iconMap[type] || '';
    var card = createEl('div', { className: '__mb-result-card __mb-result-' + type });

    var iconEl = createEl('div', { className: '__mb-result-icon' }, [icon]);
    card.appendChild(iconEl);

    if (opts.title) {
      card.appendChild(createEl('div', { className: '__mb-result-title' }, [opts.title]));
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

  // ─── Shared UI Builder ───────────────────────────────────────────────────────

  function buildWidget(options, container) {
    var relayUrl = options.relayUrl;
    var apiKey = options.apiKey;
    var amount = options.amount;
    var prefilledRef = options.reference || '';
    var currencySymbol = options.currencySymbol || DEFAULT_CURRENCY;
    var onSuccess = options.onSuccess || function () {};
    var onFailure = options.onFailure || function () {};

    var refInput, amountDisplay, btn, resultEl, hintEl, loadingEl, verifyFn;

    function showResult(type, opts) {
      clearLoading();
      resultEl.innerHTML = '';
      opts = opts || {};
      opts.currencySymbol = currencySymbol;
      resultEl.appendChild(buildResultCard(type, opts));
    }

    function clearLoading() {
      if (loadingEl && loadingEl.parentNode) loadingEl.parentNode.removeChild(loadingEl);
      btn.disabled = false;
      btn.textContent = 'Verify Payment';
    }

    function setLoading() {
      if (!loadingEl)
        loadingEl = createEl('div', { className: '__mb-loading' }, [
          createEl('div', { className: '__mb-spinner' }),
          document.createTextNode('Verifying with your phone\u2026'),
        ]);
      resultEl.innerHTML = '';
      container.appendChild(loadingEl);
      btn.disabled = true;
      btn.textContent = 'Verifying\u2026';
    }

    function handleVerify() {
      var ref = refInput.value.trim();
      if (!ref) {
        refInput.className = '__mb-error-input';
        showResult('error', {
          title: 'Reference Required',
          message: 'Please enter the transaction reference from your SMS.',
          retry: function () { refInput.focus(); resultEl.innerHTML = ''; },
        });
        refInput.focus();
        return;
      }
      refInput.className = '';
      setLoading();
      verify(
        relayUrl,
        apiKey,
        ref,
        amount,
        function (data) {
          var txn = data.transaction || {};
          var senderName = txn.senderName || null;
          var msg = 'Your payment has been verified successfully.';
          if (senderName) {
            msg += ' (from ' + senderName + ')';
          }
          showResult('success', {
            title: 'Payment Confirmed',
            amount: amount ? amount.toFixed(2) : null,
            reference: ref,
            message: msg,
          });
          notifyParent(data);
          onSuccess(data);
        },
        function (data) {
          var msg = friendlyMessage(data);
          var msgLower = msg.toLowerCase();
          var type = 'error';
          var title = 'Verification Failed';
          if (msgLower.indexOf('already') !== -1) {
            type = 'warn';
            title = 'Already Confirmed';
          }
          showResult(type, {
            title: title,
            amount: amount ? amount.toFixed(2) : null,
            reference: ref,
            message: msg,
            retry: function () { resultEl.innerHTML = ''; },
          });
          notifyParent(data);
          onFailure(data);
        }
      );
    }

    verifyFn = handleVerify;

    // Build UI
    refInput = createEl('input', {
      type: 'text',
      placeholder: 'e.g. 0000013331054115',
      value: prefilledRef,
    });
    refInput.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') handleVerify();
    });
    refInput.addEventListener('input', function () {
      refInput.className = '';
    });

    amountDisplay = createEl('div', { id: '__mb-amount-display' }, [
      document.createTextNode('Amount to verify'),
      createEl('span', {}, [currencySymbol + ' ' + (amount ? amount.toFixed(2) : '0.00')]),
    ]);

    btn = createEl('button', { id: '__mb-btn' }, ['Verify Payment']);
    btn.addEventListener('click', handleVerify);

    resultEl = createEl('div', { id: '__mb-result' });

    hintEl = createEl('div', { className: '__mb-hint' }, [
      document.createTextNode(
        'Send the exact amount via MoMo to the store number, then enter the reference from the SMS above.'
      ),
    ]);

    container.appendChild(refInput);
    container.appendChild(amountDisplay);
    container.appendChild(btn);
    container.appendChild(resultEl);
    container.appendChild(hintEl);
  }

  // ─── Popup Mode ─────────────────────────────────────────────────────────────

  function popup(options) {
    injectStyles();
    if (!options || !options.relayUrl || !options.apiKey) {
      console.error('[MoMoBridge] relayUrl and apiKey are required');
      return;
    }

    // Overlay
    var overlay = createEl('div', { id: '__mb-overlay' });
    var modal = createEl('div', { id: '__mb-modal' });

    // Header
    var closeBtn = createEl('button', { id: '__mb-close' }, ['\u00D7']);
    var header = createEl('div', { id: '__mb-header' }, [
      createEl('h3', {}, [options.title || 'MoMo Bridge']),
      closeBtn,
    ]);

    // Body
    var body = createEl('div', { id: '__mb-body' });
    var label = createEl('label', {}, ['Transaction Reference']);

    // Build widget inside body
    body.appendChild(label);
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
    injectStyles();
    if (!options || !options.relayUrl || !options.apiKey) {
      console.error('[MoMoBridge] relayUrl and apiKey are required');
      return;
    }
    var container = queryEl(options.container);
    if (!container) {
      console.error('[MoMoBridge] container not found:', options.container);
      return;
    }

    var wrapper = createEl('div', { className: '__mb-widget-wrapper', style: { maxWidth: '400px', margin: '0 auto' } });
    container.appendChild(wrapper);

    var label = createEl('label', {}, ['Transaction Reference']);
    wrapper.appendChild(label);

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
      '&amount=' + encodeURIComponent(options.amount || '') +
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

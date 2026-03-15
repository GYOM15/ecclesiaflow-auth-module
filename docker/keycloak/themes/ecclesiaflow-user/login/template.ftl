<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html<#if realm.internationalizationEnabled> lang="${locale.currentLanguageTag}"</#if>>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>
<body class="${bodyClass}">

    <#if bodyClass == "page-voiles">
    <!-- ============================================================
         SPLIT LAYOUT (login / register)
         Left panel: VoilesAngulaires + branding + trust indicators
         Right panel: form
         ============================================================ -->
    <div class="split">
        <!-- Left branding panel -->
        <div class="panel-left">
            <canvas id="bg-canvas" class="bg-canvas" aria-hidden="true"></canvas>

            <#if realm.internationalizationEnabled && locale.supported?size gt 1>
                <div class="locale">
                    <select onchange="window.location=this.value">
                        <#list locale.supported as l>
                            <option value="${l.url}"<#if l.label == locale.current> selected</#if>>${l.label}</option>
                        </#list>
                    </select>
                </div>
            </#if>

            <div class="pl-content">
                <div class="pl-logo">Ecclesia<span class="flow">Flow</span></div>
                <div class="pl-tagline">${msg("brandTagline")}</div>

                <div class="pl-trust">
                    <div class="pl-trust-item">
                        <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"/></svg>
                        ${msg("trustEncrypted")}
                    </div>
                    <div class="pl-trust-item">
                        <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M9 12.75 11.25 15 15 9.75m-3-7.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285Z"/></svg>
                        ${msg("trustSecure")}
                    </div>
                    <div class="pl-trust-item">
                        <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/></svg>
                        ${msg("trustAvailable")}
                    </div>
                </div>
            </div>
        </div>

        <!-- Right form panel -->
        <div class="panel-right">
            <div class="corner-tl"></div>
            <div class="corner-br"></div>

            <div class="form-container">
                <div class="mobile-logo">
                    <div class="logo-text">Ecclesia<span class="flow">Flow</span></div>
                </div>

                <#nested "welcome">
                <#nested "header">

                <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                    <div class="alert alert-${message.type}">
                        <#if message.type = 'success'>
                            <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/></svg>
                        <#elseif message.type = 'error'>
                            <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"/></svg>
                        <#elseif message.type = 'warning'>
                            <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"/></svg>
                        <#else>
                            <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"/></svg>
                        </#if>
                        <span>${kcSanitize(message.summary)?no_esc}</span>
                    </div>
                </#if>

                <#nested "form">

                <#nested "socialProviders">

                <#if displayInfo>
                    <#nested "info">
                </#if>

                <!-- Back to home link -->
                <div class="usr-back-home-wrap">
                    <a href="${msg("landingUrl")}" class="usr-back-home">
                        <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                        ${msg("backToHome")}
                    </a>
                </div>
            </div>
        </div>
    </div>

    <#else>
    <!-- ============================================================
         CENTERED CARD LAYOUT (reset password, other pages)
         ============================================================ -->
    <div class="user-centered">
        <canvas id="bg-canvas" class="bg-canvas" aria-hidden="true"></canvas>

        <#if realm.internationalizationEnabled && locale.supported?size gt 1>
            <div class="user-locale">
                <select onchange="window.location=this.value">
                    <#list locale.supported as l>
                        <option value="${l.url}"<#if l.label == locale.current> selected</#if>>${l.label}</option>
                    </#list>
                </select>
            </div>
        </#if>

        <div class="user-content">
            <!-- Back to home link -->
            <a href="${msg("landingUrl")}" class="usr-back-home">
                <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                ${msg("backToHome")}
            </a>

            <div class="user-logo">Ecclesia<span class="flow">Flow</span></div>

            <#nested "welcome">

            <div class="user-card">
                <div class="fcard">
                    <#nested "header">

                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="alert alert-${message.type}">
                            <#if message.type = 'success'>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"/></svg>
                            <#elseif message.type = 'error'>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"/></svg>
                            <#elseif message.type = 'warning'>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"/></svg>
                            <#else>
                                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"/></svg>
                            </#if>
                            <span>${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <#nested "form">

                    <#nested "socialProviders">

                    <#if displayInfo>
                        <#nested "info">
                    </#if>
                </div>
            </div>

            <!-- Trust indicators -->
            <div class="usr-trust">
                <div class="usr-trust-item">
                    <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"/></svg>
                    ${msg("trustEncrypted")}
                </div>
                <div class="usr-trust-item">
                    <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M9 12.75 11.25 15 15 9.75m-3-7.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285Z"/></svg>
                    ${msg("trustSecure")}
                </div>
                <div class="usr-trust-item">
                    <svg fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/></svg>
                    ${msg("trustAvailable")}
                </div>
            </div>

            <!-- Bottom tagline -->
            <div class="usr-bottom-tag">${msg("brandSlogan")}</div>
        </div>
    </div>
    </#if>

    <script>
    /* Password visibility toggle */
    document.querySelectorAll('.pw-toggle').forEach(function(btn){btn.addEventListener('click',function(){var input=document.getElementById(this.getAttribute('data-target'));if(input.type==='password'){input.type='text';this.classList.add('is-visible');this.setAttribute('aria-label','Hide password');}else{input.type='password';this.classList.remove('is-visible');this.setAttribute('aria-label','Show password');}});});
    </script>

    <#if bodyClass == "page-voiles">
    <script>
    /* VoilesAngulaires — diagonal animated panels */
    (function(){
      var c=document.getElementById('bg-canvas');if(!c)return;
      var ctx=c.getContext('2d');if(!ctx)return;
      var BG='#F8FAFC';
      var P=[
        {co:'#C7D2FE',a:-28,x:.30,w:200,s:.4},
        {co:'#A5B4FC',a:-24,x:.40,w:180,s:.6},
        {co:'#818CF8',a:-32,x:.50,w:220,s:.35},
        {co:'#6366F1',a:-26,x:.60,w:250,s:.5},
        {co:'#4F46E5',a:-30,x:.70,w:200,s:.7},
        {co:'#4338CA',a:-22,x:.55,w:170,s:.45},
        {co:'#7C3AED',a:-35,x:.75,w:160,s:.55},
        {co:'#14B8A6',a:-20,x:.85,w:140,s:.65}
      ];
      if(window.matchMedia('(prefers-reduced-motion:reduce)').matches){drawStatic();return;}
      var aid,st=null;
      function resize(){var d=window.devicePixelRatio||1;var r=c.getBoundingClientRect();c.width=r.width*d;c.height=r.height*d;ctx.setTransform(d,0,0,d,0,0);}
      function anim(ts){if(!st)st=ts;var t=ts-st;var r=c.getBoundingClientRect();var w=r.width,h=r.height;
        ctx.fillStyle=BG;ctx.fillRect(0,0,w,h);
        P.forEach(function(p,i){
          var px=w*p.x+Math.sin(t*0.0004*p.s+i*0.8)*60;
          var rot=(p.a+Math.sin(t*0.00025+i*1.2)*2)*Math.PI/180;
          var op=0.35+Math.sin(t*0.0003+i*0.6)*0.05;
          ctx.save();ctx.translate(px,h/2);ctx.rotate(rot);ctx.globalAlpha=op;
          ctx.fillStyle=p.co;ctx.fillRect(-p.w/2,-h,p.w,h*2);
          ctx.fillStyle='rgba(0,0,0,0.04)';
          ctx.fillRect(-p.w/2,-h,3,h*2);ctx.fillRect(-p.w/2+3,-h,3,h*2);
          ctx.fillRect(p.w/2-6,-h,3,h*2);ctx.fillRect(p.w/2-3,-h,3,h*2);
          ctx.restore();
        });
        aid=requestAnimationFrame(anim);
      }
      function drawStatic(){resize();var r=c.getBoundingClientRect();var w=r.width,h=r.height;
        ctx.fillStyle=BG;ctx.fillRect(0,0,w,h);
        P.forEach(function(p){var px=w*p.x;var rot=p.a*Math.PI/180;
          ctx.save();ctx.translate(px,h/2);ctx.rotate(rot);ctx.globalAlpha=0.35;
          ctx.fillStyle=p.co;ctx.fillRect(-p.w/2,-h,p.w,h*2);
          ctx.fillStyle='rgba(0,0,0,0.04)';
          ctx.fillRect(-p.w/2,-h,3,h*2);ctx.fillRect(-p.w/2+3,-h,3,h*2);
          ctx.fillRect(p.w/2-6,-h,3,h*2);ctx.fillRect(p.w/2-3,-h,3,h*2);
          ctx.restore();
        });
      }
      resize();aid=requestAnimationFrame(anim);
      window.addEventListener('resize',resize);
    })();
    </script>
    <#else>
    <script>
    /* WaveRibbon — flowing ribbon lines (accentuated for white bg) */
    (function(){
      var c=document.getElementById('bg-canvas');if(!c)return;
      var ctx=c.getContext('2d');if(!ctx)return;
      if(window.matchMedia('(prefers-reduced-motion:reduce)').matches)return;
      var NUM=65,W=0,H=0;
      var C=[[45,212,191],[129,140,248],[124,58,237],[79,70,229]];
      function resize(){var d=window.devicePixelRatio||1;var r=c.getBoundingClientRect();W=r.width;H=r.height;c.width=W*d;c.height=H*d;ctx.setTransform(d,0,0,d,0,0);}
      function rcy(sx){return H*(0.85-sx*0.13)+(-H*0.45*Math.pow(Math.sin(sx*Math.PI*0.8),1.5))+(H*0.06*Math.sin(sx*Math.PI*1.6));}
      function rw(sx){return 40+sx*sx*H*0.9;}
      function frame(time){if(W===0||H===0){requestAnimationFrame(frame);return;}
        ctx.clearRect(0,0,W,H);var t=time*0.001;
        for(var i=0;i<NUM;i++){var lt=i/(NUM-1);var off=(lt-0.5)*2;
          ctx.beginPath();ctx.lineWidth=0.6+(1-Math.abs(off))*0.4;
          for(var s=0;s<=180;s++){var sx=s/180;var x=sx*W*1.2-W*0.1;
            var cy=rcy(sx);var w=rw(sx);
            var tw=Math.sin(sx*Math.PI+t*0.35)*0.25+Math.sin(sx*Math.PI*0.5+t*0.2+1)*0.15;
            var y=cy+Math.cos(tw)*off*w*0.5;
            if(s===0)ctx.moveTo(x,y);else ctx.lineTo(x,y);
          }
          var dp=Math.abs(off);var cb=1-dp;var bo=0.08+cb*0.4;var br=0.6+cb*0.4;
          var fs=H*0.33,fe=H*0.85,my=rcy(0.5);var vf=1;
          if(my>fs){var pg=Math.min((my-fs)/(fe-fs),1);vf=1-pg*pg;}
          var g=ctx.createLinearGradient(0,0,W,0);
          g.addColorStop(0,'rgba('+Math.round(C[0][0]*br)+','+Math.round(C[0][1]*br)+','+Math.round(C[0][2]*br)+','+(bo*0.4*vf)+')');
          g.addColorStop(0.12,'rgba('+Math.round(C[0][0]*br)+','+Math.round(C[0][1]*br)+','+Math.round(C[0][2]*br)+','+(bo*0.8*vf)+')');
          g.addColorStop(0.35,'rgba('+Math.round(C[1][0]*br)+','+Math.round(C[1][1]*br)+','+Math.round(C[1][2]*br)+','+(bo*vf)+')');
          g.addColorStop(0.55,'rgba('+Math.round(C[2][0]*br)+','+Math.round(C[2][1]*br)+','+Math.round(C[2][2]*br)+','+(bo*vf)+')');
          g.addColorStop(0.8,'rgba('+Math.round(C[3][0]*br)+','+Math.round(C[3][1]*br)+','+Math.round(C[3][2]*br)+','+(bo*0.85*vf)+')');
          g.addColorStop(1,'rgba('+Math.round(C[3][0]*br)+','+Math.round(C[3][1]*br)+','+Math.round(C[3][2]*br)+','+(bo*0.4*vf)+')');
          ctx.strokeStyle=g;ctx.stroke();
        }
        requestAnimationFrame(frame);
      }
      resize();requestAnimationFrame(frame);
      window.addEventListener('resize',resize);
    })();
    </script>
    </#if>
</body>
</html>
</#macro>

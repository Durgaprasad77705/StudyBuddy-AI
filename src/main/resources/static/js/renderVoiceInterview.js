// ---------------------------------------------------------------------------
// INVALID LIVE INTERVIEW ROUTE HANDLER
// ---------------------------------------------------------------------------

async function renderInvalidLiveRoute() {

  clearInterval(timerInterval);

  stopLiveRecognition();

  if (window.speechSynthesis) {
    window.speechSynthesis.cancel();
  }

  if (liveMediaStream) {

    liveMediaStream
      .getTracks()
      .forEach(track => track.stop());

    liveMediaStream = null;
  }

  liveState = 'ERROR';
  liveCompleted = false;
  liveTranscript = '';
  liveTranscriptHistory = [];
  liveProcessing = false;
  liveAiSpeaking = false;

  container.innerHTML = `
    <div class="card" style="text-align:center">

      <div style="font-size:52px;margin-bottom:12px">
        &#9888;
      </div>

      <h2>Live Interview Session Not Found</h2>

      <p style="color:var(--muted);max-width:650px;margin:0 auto 20px">
        The Live Interview requires a valid interview session.
        A session ID was missing or invalid, so the request was
        safely blocked before reaching the server.
      </p>

      <div
        class="msg error"
        style="text-align:left;max-width:650px;margin:0 auto 20px">

        Invalid session ID

      </div>

      <button
        class="btn"
        id="recoverLiveInterviewBtn">

        Start New Live Interview

      </button>

    </div>
  `;

  document
    .getElementById('recoverLiveInterviewBtn')
    .addEventListener('click', () => {

      navigate('#/start');

    });
}


// ---------------------------------------------------------------------------
// LIVE INTERVIEW
// ---------------------------------------------------------------------------

async function renderVoiceInterview(sessionId) {

  clearInterval(timerInterval);

  // -------------------------------------------------------------------------
  // VALIDATE SESSION ID
  // -------------------------------------------------------------------------

  if (
    sessionId === undefined ||
    sessionId === null ||
    sessionId === '' ||
    String(sessionId).toLowerCase() === 'undefined' ||
    String(sessionId).toLowerCase() === 'null' ||
    !/^\d+$/.test(String(sessionId))
  ) {

    console.error(
      'Invalid Live Interview sessionId:',
      sessionId
    );

    return renderInvalidLiveRoute();
  }


  const numericSessionId =
    Number(sessionId);


  if (
    !Number.isSafeInteger(numericSessionId) ||
    numericSessionId <= 0
  ) {

    console.error(
      'Unsafe Live Interview session ID:',
      sessionId
    );

    return renderInvalidLiveRoute();
  }


  // -------------------------------------------------------------------------
  // LOAD SESSION
  // -------------------------------------------------------------------------

  try {

    if (
      !activeSession ||
      Number(activeSession.id) !== numericSessionId
    ) {

      activeSession =
        await Api.get(
          '/interviews/' + numericSessionId
        );
    }


    if (!activeSession) {

      throw new Error(
        'Interview session could not be loaded.'
      );
    }


    // -----------------------------------------------------------------------
    // VALIDATE QUESTIONS
    // -----------------------------------------------------------------------

    if (
      !Array.isArray(
        activeSession.questions
      )
    ) {

      throw new Error(
        'Interview session contains no questions.'
      );
    }


    // -----------------------------------------------------------------------
    // FIND CURRENT QUESTION
    // -----------------------------------------------------------------------

    currentQuestionIdx =
      activeSession.questions.findIndex(
        question =>
          !question.answered
      );


    if (currentQuestionIdx === -1) {

      currentQuestionIdx =
        activeSession.questions.length;
    }


    // -----------------------------------------------------------------------
    // IMPORTANT
    // -----------------------------------------------------------------------
    // KEEP YOUR EXISTING LIVE INTERVIEW CODE BELOW HERE.
    //
    // For example:
    //
    // liveTranscriptHistory = [];
    // liveTranscript = '';
    // liveProcessing = false;
    // liveAiSpeaking = false;
    //
    // ... camera
    // ... microphone
    // ... speech recognition
    // ... AI question
    // ... timer
    // ... answer submission
    //
    // DO NOT create another:
    //
    // async function renderVoiceInterview(sessionId)
    //
    // -----------------------------------------------------------------------


    liveTranscriptHistory = [];
    liveTranscript = '';
    liveProcessing = false;
    liveAiSpeaking = false;
    liveCompleted = false;
    liveState = 'READY';


    // -----------------------------------------------------------------------
    // CONTINUE YOUR EXISTING UI CODE
    // -----------------------------------------------------------------------

    // IMPORTANT:
    // Paste your OLD renderVoiceInterview() code here from the point
    // where your original code starts rendering the Live Interview UI.


  } catch (error) {

    console.error(
      'Live Interview loading error:',
      error
    );

    liveState = 'ERROR';

    container.innerHTML = `

      <div class="card">

        <div class="msg error">

          ${escapeHtml(
            error?.message ||
            'Unable to load Live Interview.'
          )}

        </div>

        <button
          class="btn"
          id="retryLiveInterviewBtn">

          Start New Live Interview

        </button>

      </div>

    `;


    document
      .getElementById(
        'retryLiveInterviewBtn'
      )
      ?.addEventListener(
        'click',
        () => {

          navigate('#/start');

        }
      );
  }
}
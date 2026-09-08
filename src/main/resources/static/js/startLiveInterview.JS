async function startLiveInterview() {

  if (!activeSession?.id) {

    updateVoiceState(
      'ERROR',
      'Interview session is missing. Please start a new interview.'
    );

    return;
  }

  if (
    !Number.isSafeInteger(Number(activeSession.id)) ||
    Number(activeSession.id) <= 0
  ) {

    updateVoiceState(
      'ERROR',
      'Invalid interview session.'
    );

    return;
  }

  try {

    updateVoiceState(
      'INITIALIZING',
      'Initializing camera and microphone...'
    );

    await requestLiveMedia();

    timerSeconds = 0;

    clearInterval(timerInterval);

    timerInterval = setInterval(
      renderVoiceTimer,
      1000
    );

    liveQuestionStart = Date.now();

    updateVoiceState(
      'READY',
      'Live interview initialized successfully.'
    );

    await askCurrentVoiceQuestion();

  } catch (err) {

    console.error(
      'Live interview initialization failed:',
      err
    );

    clearInterval(timerInterval);

    updateVoiceState(
      'ERROR',
      err?.message ||
      'Unable to start media devices.'
    );

  }
}
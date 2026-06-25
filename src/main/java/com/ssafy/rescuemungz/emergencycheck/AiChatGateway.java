package com.ssafy.rescuemungz.emergencycheck;

import java.util.Optional;

interface AiChatGateway {
    Optional<String> completeJson(String userPrompt);
}

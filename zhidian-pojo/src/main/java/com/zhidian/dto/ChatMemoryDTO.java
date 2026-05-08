package com.zhidian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemoryDTO {

  private long id;
  private String sessionId;
  private String messageType;
  private String content;
}

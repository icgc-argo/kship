package org.icgcargo.kship;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ManagementController {

  @GetMapping(value = "/")
  @ResponseStatus(HttpStatus.OK)
  public Map<String, String> ping() {
    val response = new HashMap<String, String>();
    response.put("status", "up");
    return response;
  }
}

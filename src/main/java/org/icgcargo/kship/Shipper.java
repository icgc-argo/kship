package org.icgcargo.kship;

import com.fasterxml.jackson.databind.JsonNode;

public interface Shipper {
    void ship(JsonNode message);
}

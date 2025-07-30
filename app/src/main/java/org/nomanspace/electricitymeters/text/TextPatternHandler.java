package org.nomanspace.electricitymeters.text;

import org.nomanspace.electricitymeters.model.Concentrator;

import java.util.List;

public interface TextPatternHandler {

    List<Concentrator> process(List<String> input);
}

package org.jboss.pnc.bifrost.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.pnc.bifrost.common.ObjectMapperProvider;
import org.jboss.pnc.bifrost.source.dto.Line;
import org.jboss.pnc.bifrost.test.DebugTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@DebugTest
public class TestSerialization {

    @Test
    public void shouldSerializeAndDeserializeLine() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Line line1 = new Line(
                "1",
                "1548633600000",
                this.getClass().getName(),
                "line1",
                false,
                "dsfdo",
                false,
                "111111"
        );

        String asString = objectMapper.writeValueAsString(line1);

        System.out.println(asString);

        Line line = objectMapper.readValue(asString, Line.class);

        byte[] byteLine = objectMapper.writeValueAsBytes(line1);
        System.out.println("Byteline: " + new String(byteLine));

    }


    @Test
    public void shouldSerializeOptional() throws IOException {
        ObjectMapper mapper = ObjectMapperProvider.get();

        Optional<String> original = Optional.of("chameleon");

        String string = mapper.writeValueAsString(original);

        System.out.println(string);

        Optional<String> deserialized = mapper.readValue(string, Optional.class);
        Assertions.assertEquals(original, deserialized);
    }

    @Test
    public void shouldSerializeOptional2() throws IOException {
        ObjectMapper mapper = ObjectMapperProvider.get();

        Wrapper original = new Wrapper(Optional.of("chameleon"));

        String string = mapper.writeValueAsString(original);
        System.out.println(string);

        Wrapper deserialized = mapper.readValue(string, Wrapper.class);
        Assertions.assertEquals(original, deserialized);
    }

    public static class Wrapper {
        Optional<String> animal;

        public Wrapper() {
        }

        public Wrapper(Optional<String> animal) {
            this.animal = animal;
        }

        public Optional<String> getAnimal() {
            return animal;
        }

        public void setAnimal(Optional<String> animal) {
            this.animal = animal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Wrapper))
                return false;
            Wrapper wrapper = (Wrapper) o;
            return Objects.equals(animal, wrapper.animal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(animal);
        }
    }


}

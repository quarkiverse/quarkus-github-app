package io.quarkiverse.githubapp.it.testingframework.config;

import java.util.Objects;

public class MyConfigFile {

    public String someProperty;

    public MyConfigFile() {

    }

    public MyConfigFile(String someProperty) {
        this.someProperty = someProperty;
    }

    @Override
    public String toString() {
        return "MyConfigFile{" +
                "someProperty='" + someProperty + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MyConfigFile that = (MyConfigFile) o;
        return Objects.equals(someProperty, that.someProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(someProperty, someProperty);
    }

}

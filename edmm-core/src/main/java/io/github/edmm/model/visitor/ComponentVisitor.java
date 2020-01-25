package io.github.edmm.model.visitor;

import io.github.edmm.model.component.*;

public interface ComponentVisitor {

    default void visit(Auth0 component) {
        // noop
    }

    default void visit(AwsAurora component) {
        // noop
    }

    default void visit(AwsBeanstalk component) {
        // noop
    }

    default void visit(Compute component) {
        // noop
    }

    default void visit(Database component) {
        // noop
    }

    default void visit(Dbaas component) {
        // noop
    }

    default void visit(Dbms component) {
        // noop
    }

    default void visit(MysqlDatabase component) {
        // noop
    }

    default void visit(MysqlDbms component) {
        // noop
    }

    default void visit(Paas component) {
        // noop
    }

    default void visit(Platform component) {
        // noop
    }

    default void visit(RootComponent component) {
        // noop
    }

    default void visit(Saas component) {
        // noop
    }

    default void visit(SoftwareComponent component) {
        // noop
    }

    default void visit(Tomcat component) {
        // noop
    }

    default void visit(WebApplication component) {
        // noop
    }

    default void visit(WebServer component) {
        // noop
    }
}


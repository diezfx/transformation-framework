package io.github.edmm.model.support;

import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.ConnectsTo;
import io.github.edmm.model.relation.DependsOn;
import io.github.edmm.model.relation.HostedOn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class TypeResolver {

    private static final Logger logger = LoggerFactory.getLogger(TypeResolver.class);

    private static final Map<String, Class<? extends ModelEntity>> TYPE_MAPPING = new HashMap<>();

    static {
        // Components
        put("base", RootComponent.class);
        put("compute", Compute.class);
        put("software_component", SoftwareComponent.class);
        put("web_server", WebServer.class);
        put("web_application", WebApplication.class);
        put("dbms", Dbms.class);
        put("database", Database.class);
        put("tomcat", Tomcat.class);
        put("mysql_dbms", MysqlDbms.class);
        put("mysql_database", MysqlDatabase.class);
        put("platform", Platform.class);
        put("paas", Paas.class);
        put("dbaas", Dbaas.class);
        put("aws_beanstalk", AwsBeanstalk.class);
        put("aws_aurora", AwsAurora.class);
        put("saas", Saas.class);
        put("auth0", Auth0.class);
        // Relations
        put("depends_on", DependsOn.class);
        put("hosted_on", HostedOn.class);
        put("connects_to", ConnectsTo.class);
    }

    public static Class<? extends ModelEntity> resolve(String type) {
        Class<? extends ModelEntity> clazz = TYPE_MAPPING.get(type);
        if (clazz != null) {
            return clazz;
        } else {
            logger.warn("Type '{}' is unknown and not supported", type);
            return RootComponent.class;
        }
    }

    private static void put(String name, Class<? extends ModelEntity> clazz) {
        TYPE_MAPPING.put(name, clazz);
    }
}

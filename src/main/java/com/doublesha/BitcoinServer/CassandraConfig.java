package com.doublesha.BitcoinServer;

import org.springframework.cassandra.core.CqlTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;

@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Override
    public String getKeyspaceName() {
        return "dblsha";
    }

    @Bean
    public CqlTemplate cqlTemplate() throws Exception {
        return new CqlTemplate(session().getObject());
    }
}

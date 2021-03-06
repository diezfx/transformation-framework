package io.github.edmm.plugins.chef;

import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.support.CheckModelResult;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.visitor.VisitorHelper;
import io.github.edmm.plugins.ComputeSupportVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChefLifecycle extends AbstractLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(ChefLifecycle.class);

    public static final String COOKBOOKS_FOLDER = "cookbooks";
    public static final String POLICIES_FOLDER = "policies";
    public static final String POLICY_FILENAME = "policyfile.rb";
    public static final String COOKBOOK_RECIPES_FOLDER = "recipes";
    public static final String COOKBOOK_FILES_FOLDER = "files";
    public static final String COOKBOOK_ATTRIBUTES_FOLDER = "attributes";
    public static final String COOKBOOK_DEFAULT_RECIPE_FILENAME = "default.rb";
    public static final String COOKBOOK_METADATA_FILENAME = "metadata.rb";
    public static final String COOKBOOK_CHEFIGNORE_FILENAME = "chefignore";

    public ChefLifecycle(TransformationContext context) {
        super(context);
    }

    @Override
    public CheckModelResult checkModel() {
        ComputeSupportVisitor visitor = new ComputeSupportVisitor(context);
        VisitorHelper.visit(context.getModel().getComponents(), visitor);
        return visitor.getResult();
    }

    @Override
    public void transform() {
        logger.info("Begin transformation to Chef...");
        ChefTransformer transformer = new ChefTransformer(context);
        transformer.populateChefRepository();
        logger.info("Transformation to Chef successful");
    }
}

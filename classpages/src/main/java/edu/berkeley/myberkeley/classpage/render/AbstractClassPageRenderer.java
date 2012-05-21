package edu.berkeley.myberkeley.classpage.render;


import org.sakaiproject.nakamura.api.lite.Repository;


abstract class AbstractClassPageRenderer implements ClassPageRenderer{
  
  protected Repository respository;
  
  // to allow passing in of template from a file later
  protected String template;
  
  protected AbstractClassPageRenderer(Repository repository, String template) {
    this.respository = repository;
    this.template = template;
  }
  
}

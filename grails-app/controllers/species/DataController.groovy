package species

import java.sql.ResultSet;

import species.TaxonomyDefinition.TaxonomyRank;
import species.formatReader.SpreadsheetReader;
import species.sourcehandler.MappedSpreadsheetConverter;
import species.sourcehandler.XMLConverter;
import grails.converters.JSON;
import grails.converters.XML;
import grails.plugins.springsecurity.Secured;
import grails.web.JSONBuilder;
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql
import groovy.xml.MarkupBuilder;
import java.util.List;
import java.util.Map;


class DataController {

	def dataSource

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
	def combinedHierarchy = Classification.findByName(grailsApplication.config.speciesPortal.fields.COMBINED_TAXONOMIC_HIERARCHY);

	/**
	 * 
	 */
	def index = {
	}

	/**
	 * 
	 */
	def listHierarchy = {
		log.debug params;
		//cache "taxonomy_results"
		includeOriginHeader();

		int level = params.n_level ? Integer.parseInt(params.n_level)+1 : 0
		def parentId = params.nodeid  ?: null
		def expandAll = params.expand_all  ? (new Boolean(params.expand_all)).booleanValue(): false
		def expandSpecies = params.expand_species  ? (new Boolean(params.expand_species)).booleanValue(): false
		long classSystem = params.classSystem ? Long.parseLong(params.classSystem): null;
		Long speciesid = params.speciesid ? Long.parseLong(params.speciesid) : null

		combinedHierarchy.merge();
		if(classSystem == combinedHierarchy.id) {
			classSystem = null;
		}

		long startTime = System.currentTimeMillis();
		def rs = new ArrayList<GroovyRowResult>();
		if(expandSpecies) {
			//def taxonIds = getSpeciesHierarchyTaxonIds(speciesid, classSystem)
			//getHierarchyNodes(rs, 0, 8, null, classSystem, false, expandSpecies, taxonIds);
			getSpeciesHierarchy(speciesid, rs, classSystem);
		} else {
			getHierarchyNodes(rs, level, level+3, parentId, classSystem, expandAll, expandSpecies, null);
		}
		log.debug "Time taken to build hierarchy : ${(System.currentTimeMillis()- startTime)/1000}(sec)"
		render(contentType: "text/xml", text:buildHierarchyResult(rs, classSystem))
	}

	/**
	 * 
	 * @param resultSet
	 * @param level
	 * @param parentId
	 * @param classSystem
	 * @param expandAll
	 * @param taxonIds
	 */
	private void getHierarchyNodes(List<GroovyRowResult> resultSet, int level, int tillLevel, String parentId, Long classSystem, boolean expandAll, boolean expandSpecies, List taxonIds) {
		def sql = new Sql(dataSource)
		def rs;
		String sqlStr;
		
		long startTime = System.currentTimeMillis();
		
		if(classSystem) {
			if(!parentId) {
				sqlStr = "select t.id as taxonid, 1 as count, t.rank as rank, t.name as name, s.path as path, ${classSystem} as classsystem \
							from taxonomy_registry s, \
								taxonomy_definition t \
							where \
								s.taxon_definition_id = t.id and "+
								(classSystem?"s.classification_id = :classSystem and ":"")+
								"t.rank = 0";
				rs = sql.rows(sqlStr, [classSystem:classSystem])
			}
			else if(level == TaxonomyRank.SPECIES.ordinal()) {
				sqlStr = "select t.id as taxonid,  1 as count, t.rank as rank, t.name as name,  s.path as path , ${classSystem} as classsystem\
							from taxonomy_registry s, taxonomy_definition t \
							where \
								s.taxon_definition_id = t.id and "+
										(classSystem?"s.classification_id = :classSystem and ":"")+
										+"t.rank = "+level+" and \
								s.path ~ '^"+parentId+"_[0-9]+\$' "
				rs = sql.rows(sqlStr , [classSystem:classSystem]);
			} else {
				sqlStr = "select t.id as taxonid, 1 as count, t.rank as rank, t.name as name,  s.path as path , ${classSystem} as classsystem\
							from taxonomy_registry s, \
								taxonomy_definition t \
							where \
								s.taxon_definition_id = t.id and "+
								(classSystem?"s.classification_id = :classSystem and ":"")+
								"s.path ~ '^"+parentId+"_[0-9]+\$' " +
								"order by t.rank, t.name asc";
				rs = sql.rows(sqlStr, [classSystem:classSystem])
			}
		} else {
			if(!parentId) {
				sqlStr = "select t.id as taxonid, 1 as count, 0 as rank, t.name as name, s.path as path , ${classSystem} as classsystem\
							from taxonomy_registry s, \
								taxonomy_definition t \
							where \
								s.taxon_definition_id = t.id and "+
								"t.rank = 0 group by s.path, t.id, t.name";
				rs = sql.rows(sqlStr)
			}
			else if(level == TaxonomyRank.SPECIES.ordinal()) {
				sqlStr = "select t.id as taxonid,  1 as count, t.rank as rank, t.name as name,  s.path as path , ${classSystem} as classsystem\
							from taxonomy_registry s, taxonomy_definition t \
							where \
								s.taxon_definition_id = t.id and "+
								+"t.rank = "+level+" and \
								s.path ~ '^"+parentId+"_[0-9]+\$' group by s.path, t.id, t.name";
				rs = sql.rows(sqlStr );
			} else {
				sqlStr =  "select t.id as taxonid, 1 as count, t.rank as rank, t.name as name,  s.path as path , ${classSystem} as classsystem\
							from taxonomy_registry s, \
								taxonomy_definition t \
							where \
								s.taxon_definition_id = t.id and "+
								"s.path ~ '^"+parentId+"_[0-9]+\$' group by s.path, t.rank, t.id, t.name" +
								" order by t.rank asc, t.name"
				rs = sql.rows(sqlStr)
			}
		}
		log.debug "Time taken to execute taxon hierarchy query : ${(System.currentTimeMillis()- startTime)/1000}(sec)"
		log.debug "SQL for taxon hierarchy : "+sqlStr;
		rs.each { r ->
			r.put('expanded', false);
			r.put("speciesid", -1)
			r.put('loaded', false);
			populateSpeciesDetails(r.taxonid, r);
			
			resultSet.add(r);
			if(expandAll || (taxonIds && taxonIds.contains(r.taxonid))) {
				if(r.rank < TaxonomyRank.SPECIES.ordinal()) {
					//r.put('count', getCount(r.path, classSystem));
					if(r.rank+1 <= tillLevel) {
						r.put('expanded', true);
						r.put('loaded', true);
						getHierarchyNodes(resultSet, r.rank+1, tillLevel, r.path, classSystem, expandAll, expandSpecies, taxonIds)
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param name
	 * @param taxonId
	 * @param path
	 * @param classSystem
	 * @return
	 */
	private getSpecies(long taxonId) {
		def sql = new Sql(dataSource)
		int level = TaxonomyRank.SPECIES.ordinal();
		return Species.find("from Species as s where s.taxonConcept.id = :taxonId", [taxonId:taxonId]);
	}

	/**
	 * TODO:optimize 
	 * @param id
	 * @return
	 */
	private String getSpeciesName(long id) {
		def species = Species.get(id);
		return species.taxonConcept.italicisedForm;
	}

	/**
	 * 
	 * @param speciesId
	 * @param classSystem
	 * @return
	 */
	private List getSpeciesHierarchyTaxonIds(Long taxonId, Long classSystem) {
		def sql = new Sql(dataSource)
		String s = """select s.path as path 
			from taxonomy_registry s, 
				taxonomy_definition t 
			where 
				s.taxon_definition_id = t.id and 
				${(classSystem?"s.classification_id = :classSystem and ":"")}
				s.path like '%!_"""+taxonId+"' escape '!'";

		def rs
		if(classSystem) {
			rs = sql.rows(s, [classSystem:classSystem])
		} else {
			rs = sql.rows(s)
		}
		def paths = rs.collect {it.path};


		def result = [];
		paths.each {
			it.tokenize("_").each {
				result.add(Long.parseLong(it));
			}
		}
		return result;
		//		return [Species.get(speciesId).id]
	}

	/**
	 * todo:CORRECT THIS
	 * @param speciesId
	 * @param classSystem
	 * @return
	 */
	private int getCount(String parentId, long classSystem) {
		def sql = new Sql(dataSource)
		def rs = sql.rows("select count(*) as count \
		   from taxonomy_registry s, \
		   		taxonomy_definition t \
		   where \
			   s.taxon_definition_id = t.id and "+
				(classSystem?"s.classification_id = :classSystem and ":"")+
				"s.path ~ '^"+parentId+"_[0-9_]+\$' " +
				" group by t.rank \
			having t.rank = :rank", [classSystem:classSystem, rank:TaxonomyRank.SPECIES.ordinal()])
		return rs[0]?.count;
	}

	/**
	 * 
	 */
	
	private List getSpeciesHierarchy(Long speciesTaxonId, List rs, Long classSystem) {
		List speciesHier = [];
		int minHierarchySize = 6;
		TaxonomyDefinition taxonConcept = TaxonomyDefinition.get(speciesTaxonId);
		if(classSystem) {
			Classification classification = Classification.get(classSystem);
			TaxonomyRegistry.findAllByTaxonDefinitionAndClassification(taxonConcept, classification).each {reg ->
				def list = [] 
				while(reg != null) {
					def result = ['count':1, 'rank':reg.taxonDefinition.rank, 'name':reg.taxonDefinition.name, 'path':reg.path, 'classSystem':classSystem, 'expanded':true, 'loaded':true]
					populateSpeciesDetails(speciesTaxonId, result);
					list.add(result);
					reg = reg.parentTaxon;
				}
				if(list.size() >= minHierarchySize) {
					list = list.sort {it.rank};
					speciesHier.addAll(list);
				}
			}
		} else {
			TaxonomyRegistry.findAllByTaxonDefinition(taxonConcept).each { reg ->
				def list = [];
				while(reg != null) {					
					def result = ['count':1, 'rank':reg.taxonDefinition.rank, 'name':reg.taxonDefinition.name, 'path':reg.path, 'classSystem':classSystem, 'expanded':true, 'loaded':true]
					populateSpeciesDetails(speciesTaxonId, result);
					list.add(result);					
					reg = reg.parentTaxon;
				}
				if(list.size() >= minHierarchySize) {
					list = list.sort {it.rank};
					speciesHier.addAll(list);
				}				
			}
		}
		
		//removing duplicate path elements
		def temp = new HashSet();
		speciesHier.each { map ->
			if(!temp.contains(map.path)) {
				temp.add(map.path);
				rs.add(map);
				
			}
		}
		log.debug rs;
	}
	
	/**
	 * 
	 */
	private void populateSpeciesDetails(Long speciesTaxonId, Map result) {
		if(result.rank == TaxonomyRank.SPECIES.ordinal()) {
			def species = getSpecies(speciesTaxonId);
			result.put("speciesid", species.id)
			result.put('name', species.title)
			result.put('count', 1);
		}
	}
	/**
	 * render t as XML;
	 * @param rs
	 * @param classSystem
	 * @return
	 */
	private String buildHierarchyResult(rs, classSystem) {
		def writer = new StringWriter ();
		def result = new MarkupBuilder(writer);
		int i=0;
		result.rows() {
			page (1)
			total (1)
			int size = 0;
			//t.each { taxonReg ->
			rs.each { r->
				size ++;
				String parentPath = "";
				if(r.path && r.path.lastIndexOf("_")!=-1) {
					parentPath = r.path.substring(0, r.path.lastIndexOf("_"))
				}
				row(id:r.path) {
					cell(r.path)
					cell(r.path)
					cell (r.name.trim())
					cell (r.count)
					cell (r["speciesid"])
					cell (r["classsystem"])
					cell (r.rank)
					cell (parentPath)
					cell (r.rank == TaxonomyRank.SPECIES.ordinal() ? true : false)
					cell (r.expanded?:false) //for expanded
					cell (r.loaded?:false) //for loaded
				}
			}
			records (size)
		}
		return writer.toString();
	}

	/**
	 * 
	 * @param origin
	 * @return
	 */
	private boolean isValid(String origin) {
		String originHost = (new URL(origin)).getHost();
		return grailsApplication.config.speciesPortal.validCrossDomainOrigins.contains(originHost)
	}

	/**
	 * 
	 */
	private void includeOriginHeader() {
		String origin = request.getHeader("Origin");
		if(origin) {
			String validOrigin = isValid(origin)?origin:"";
			response.setHeader("Access-Control-Allow-Origin", validOrigin);
			response.setHeader("Access-Control-Allow-Methods", request.getHeader("Access-Control-Request-Methods"));
			response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
			response.setHeader("Access-Control-Max-Age", "86400");
		}
	}

}

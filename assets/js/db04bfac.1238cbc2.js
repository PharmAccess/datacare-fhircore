"use strict";(self.webpackChunkfhircore=self.webpackChunkfhircore||[]).push([[872],{3905:(e,t,n)=>{n.d(t,{Zo:()=>c,kt:()=>m});var r=n(7294);function a(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function o(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){a(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function l(e,t){if(null==e)return{};var n,r,a=function(e,t){if(null==e)return{};var n,r,a={},i=Object.keys(e);for(r=0;r<i.length;r++)n=i[r],t.indexOf(n)>=0||(a[n]=e[n]);return a}(e,t);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(r=0;r<i.length;r++)n=i[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(a[n]=e[n])}return a}var p=r.createContext({}),s=function(e){var t=r.useContext(p),n=t;return e&&(n="function"==typeof e?e(t):o(o({},t),e)),n},c=function(e){var t=s(e.components);return r.createElement(p.Provider,{value:t},e.children)},u={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},d=r.forwardRef((function(e,t){var n=e.components,a=e.mdxType,i=e.originalType,p=e.parentName,c=l(e,["components","mdxType","originalType","parentName"]),d=s(n),m=a,f=d["".concat(p,".").concat(m)]||d[m]||u[m]||i;return n?r.createElement(f,o(o({ref:t},c),{},{components:n})):r.createElement(f,o({ref:t},c))}));function m(e,t){var n=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var i=n.length,o=new Array(i);o[0]=d;var l={};for(var p in t)hasOwnProperty.call(t,p)&&(l[p]=t[p]);l.originalType=e,l.mdxType="string"==typeof e?e:a,o[1]=l;for(var s=2;s<i;s++)o[s]=n[s];return r.createElement.apply(null,o)}return r.createElement.apply(null,n)}d.displayName="MDXCreateElement"},9919:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>p,contentTitle:()=>o,default:()=>u,frontMatter:()=>i,metadata:()=>l,toc:()=>s});var r=n(7462),a=(n(7294),n(3905));const i={},o="Configurations",l={unversionedId:"configurations/readme",id:"configurations/readme",title:"Configurations",description:"Jump to section",source:"@site/docs/configurations/readme.mdx",sourceDirName:"configurations",slug:"/configurations/",permalink:"/configurations/",draft:!1,editUrl:"https://github.com/opensrp/fhircore/tree/main/docs/docs/configurations/readme.mdx",tags:[],version:"current",frontMatter:{},sidebar:"defaultSidebar",previous:{title:"Smart Vaccination Certificates",permalink:"/advanced-fhir/smart-vax-certs"},next:{title:"Application configuration",permalink:"/configurations/application-config"}},p={},s=[{value:"Jump to section",id:"jump-to-section",level:2},{value:"Introduction",id:"introduction",level:2},{value:"Configuration Types",id:"configuration-types",level:2},{value:"Config Conventions",id:"config-conventions",level:2},{value:"FAQs",id:"faqs",level:2},{value:"Troubleshooting",id:"troubleshooting",level:2}],c={toc:s};function u(e){let{components:t,...n}=e;return(0,a.kt)("wrapper",(0,r.Z)({},c,n,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("h1",{id:"configurations"},"Configurations"),(0,a.kt)("h2",{id:"jump-to-section"},"Jump to section"),(0,a.kt)("ul",null,(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("a",{parentName:"li",href:"#introduction"},"Introduction")),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("a",{parentName:"li",href:"#configuration-types"},"Configuration Types")),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("a",{parentName:"li",href:"#config-conventions"},"Config Conventions")),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("a",{parentName:"li",href:"#faqs"},"FAQs")),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("a",{parentName:"li",href:"#troubleshooting"},"Troubleshooting"))),(0,a.kt)("h2",{id:"introduction"},"Introduction"),(0,a.kt)("p",null,"A FHIR Core application needs configurations to work properly. These configurations are used to define application workflows (or events), user interface content, which user interface component to display and which data (FHIR Resources) to load."),(0,a.kt)("p",null,"FHIR Core configs are represented using two types of FHIR resources (",(0,a.kt)("a",{parentName:"p",href:"https://build.fhir.org/composition.html"},"Composition")," and ",(0,a.kt)("a",{parentName:"p",href:"https://build.fhir.org/binary.html"},"Binary"),") which are stored in JSON format.  A Binary  resource can contain any content including custom JSONs modeled for FHIR Core's use case. These Binary resources are referenced in the ",(0,a.kt)("inlineCode",{parentName:"p"},"Composition")," resource sections; each Binary resource representing a valid configuration. A ",(0,a.kt)("inlineCode",{parentName:"p"},"Composition")," section can nested in other sections; commonly used to group related configs. "),(0,a.kt)("p",null,"A ",(0,a.kt)("inlineCode",{parentName:"p"},"Composition")," resource acts as the application  config manifest. It MUST have a unique identifier to differentiate the application from the rest of the applications sharing the same server."),(0,a.kt)("h2",{id:"configuration-types"},"Configuration Types"),(0,a.kt)("p",null,"FHIR Core supports the following types of configurations:"),(0,a.kt)("table",null,(0,a.kt)("thead",{parentName:"table"},(0,a.kt)("tr",{parentName:"thead"},(0,a.kt)("th",{parentName:"tr",align:null},"Config type"),(0,a.kt)("th",{parentName:"tr",align:null},"Function"),(0,a.kt)("th",{parentName:"tr",align:"center"},"Cardinality"))),(0,a.kt)("tbody",{parentName:"table"},(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"composition"),(0,a.kt)("td",{parentName:"tr",align:null},"Uniquely identifiers the application. References other configurations"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..1")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"application"),(0,a.kt)("td",{parentName:"tr",align:null},"Provides application level configurations e.g. languages used in the app, title of the app"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..1")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"navigation"),(0,a.kt)("td",{parentName:"tr",align:null},"Configures the app's navigation side menu (drawer). e.g. how many registers to display"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..1")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"register"),(0,a.kt)("td",{parentName:"tr",align:null},"Configures a register (what resources to use, which views to render etc)"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..*")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"profile"),(0,a.kt)("td",{parentName:"tr",align:null},"Configures a profile (what resources to use and how the views are to be rendered)"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..*")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"sync"),(0,a.kt)("td",{parentName:"tr",align:null},"Configures the resources to be synced to and from the server"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..1")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"geoWidget"),(0,a.kt)("td",{parentName:"tr",align:null},"Configures the geowidget map view"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..*")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"measureReport"),(0,a.kt)("td",{parentName:"tr",align:null},"Configures the various measure report indicators used in the app"),(0,a.kt)("td",{parentName:"tr",align:"center"},"1..1")))),(0,a.kt)("blockquote",null,(0,a.kt)("p",{parentName:"blockquote"},"NOTE: Cardinality chart: 1..1 ( only one required); 1..* (can be multiple);  0..1 (optional)")),(0,a.kt)("h2",{id:"config-conventions"},"Config Conventions"),(0,a.kt)("ul",null,(0,a.kt)("li",{parentName:"ul"},"Config attributes and types  should be in camelCase e.g. ",(0,a.kt)("inlineCode",{parentName:"li"},"application.deviceToDeviceSync")),(0,a.kt)("li",{parentName:"ul"},"Some config values are expected to be ",(0,a.kt)("inlineCode",{parentName:"li"},"enum classes"),"  constants. They MUST be provided in UPPER_SNAKE_CASE e.g the valid ",(0,a.kt)("inlineCode",{parentName:"li"},"ServiceStatus")," values include: ",(0,a.kt)("inlineCode",{parentName:"li"},"DUE"),", ",(0,a.kt)("inlineCode",{parentName:"li"},"OVERDUE"),", ",(0,a.kt)("inlineCode",{parentName:"li"},"UPCOMING")," and ",(0,a.kt)("inlineCode",{parentName:"li"},"COMPLETED")),(0,a.kt)("li",{parentName:"ul"},"Computed configuration values use ",(0,a.kt)("inlineCode",{parentName:"li"},"@{placeholder}")," format to reference the name of the computed rule. The placeholder will be replaced with an actual value at runtime.")),(0,a.kt)("h2",{id:"faqs"},"FAQs"),(0,a.kt)("ol",null,(0,a.kt)("li",{parentName:"ol"},(0,a.kt)("p",{parentName:"li"},"How do I come up with a name for the app id: ",(0,a.kt)("em",{parentName:"p"},"app Ids should be short, human readable and easy to remember. Different application should not share the same app id. Examples of good app Ids: quest, g6pd etc. You can derive an app Id after the name of the client project."))),(0,a.kt)("li",{parentName:"ol"},(0,a.kt)("p",{parentName:"li"},"Can I share configurations between two apps?: ",(0,a.kt)("em",{parentName:"p"},"yes and no. Yes when the configs are available in the same server. This is however not recommended")))),(0,a.kt)("h2",{id:"troubleshooting"},"Troubleshooting"),(0,a.kt)("ul",null,(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"Missing Resource ")," - Check whether the referenced Binary resource exists in the server. Could be a typo in the Composition resource or you forgot include the Binary resource to the Composition section."),(0,a.kt)("li",{parentName:"ul"},(0,a.kt)("inlineCode",{parentName:"li"},"Error loading configuration. Details : Key application is missing in the map")," - Composition resource for the given application ID is missing.")))}u.isMDXComponent=!0}}]);
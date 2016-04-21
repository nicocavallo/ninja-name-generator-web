package controllers

import javax.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class Application@Inject()(wsClient:WSClient) extends Controller {

  implicit val resultFormat:Format[Result] = Json.format[Result]

  val userForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(UserData.apply)(UserData.unapply)
  )

  def index = Action {
    Ok(views.html.index())
  }

  def processRequest(userData:UserData):Future[play.api.mvc.Result] =
    wsClient.url(s"http://localhost:8080/ninjaName/forUser/${userData.name}").get().map {
    case resp if resp.status == 200 =>  resp.json.validate match {
      case JsSuccess(res:Result,_) => Ok(views.html.ninjaName(res.result))
      case JsError(_) => InternalServerError("Unknown message format.")
    }
    case _ => InternalServerError("Error connecting to Ninja Name Generator API")
  }

  def createNinjaName = Action.async { implicit request =>
    userForm
      .bindFromRequest()
      .fold[Future[play.api.mvc.Result]](hasErrors = {
      formWithErrors => Future.successful(BadRequest(s"bad request, try again $formWithErrors"))
      },
        success = processRequest)
  }

}

case class UserData(name: String)
case class Result(result:String)

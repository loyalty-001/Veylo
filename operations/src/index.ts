import {app} from './application';
import {cleanup} from './public';
export default {fetch:app.fetch,async scheduled(_event:ScheduledController,env:Env){await cleanup(env.DB);}} satisfies ExportedHandler<Env>;

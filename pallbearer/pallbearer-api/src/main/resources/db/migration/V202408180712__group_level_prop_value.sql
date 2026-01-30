--
-- Update the athlete active group level property value to match new
-- conventions
--

update t_user_property
   set property_value = '1.0'
 where property_key = 'user.drill.group.level'
   and property_value = '1';

update t_user_property
   set property_value = '2.0'
 where property_key = 'user.drill.group.level'
   and property_value = '2';
